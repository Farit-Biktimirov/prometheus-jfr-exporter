package com.redhat.rhjmc.prometheus_jfr_exporter;

import io.prometheus.client.Collector;
import org.openjdk.jmc.common.IDescribable;
import org.openjdk.jmc.common.item.*;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.rjmx.services.jfr.FlightRecorderException;
import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.flightrecorder.internal.parser.v1.StructTypes.JfrThread;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class JfrCollector extends Collector { // TODO: implement Collector.Describable?
	private RecordingService mRecordingService;

	public JfrCollector() {
	}

	public JfrCollector(RecordingService rs) {
		this();

		setRecordingService(rs);
	}

	public void setRecordingService(RecordingService rs) {
		mRecordingService = rs;
	}

	@Override
	public List<MetricFamilySamples> collect() {
		try {
			return doCollect();
		} catch (Exception e) {
			// TODO: log exception
			e.printStackTrace(System.err);
			return Collections.emptyList();
		}
	}

	public List<MetricFamilySamples> doCollect()
			throws FlightRecorderException, IOException, CouldNotLoadRecordingException, EmptyRecordingException {
		if (mRecordingService == null) {
			return Collections.emptyList();
		}

		InputStream is = mRecordingService.openRecording();

		IItemCollection items = JfrLoaderToolkit.loadEvents(is);

		if (!items.hasItems()) {
			throw new EmptyRecordingException("Recording has no events recorded");
		}

		Map<IType<IItem>, List<IItemIterable>> groups = items.stream().collect(Collectors.groupingBy(IItemIterable::getType, HashMap::new, Collectors.toList()));

		final List<MetricFamilySamples> metrics = new ArrayList<>();

		groups.forEach( (type, itemIterable) -> {
			String metricName = type.getIdentifier().replaceAll("\\.", "_").replaceAll("\\$", ":");
			Collector.Type metricType = Type.GAUGE;
			String metricDescription = type.getDescription();
			Map<String,IMemberAccessor> memberAccessors = type.getAccessorKeys().entrySet().stream()
					.collect(Collectors.toMap((e)->e.getKey().getIdentifier(),(e)->type.getAccessor(e.getKey())));
			List<MetricFamilySamples.Sample> samples = new ArrayList<>();
			try {
				itemIterable.forEach( itemI -> {
					itemI.stream().forEach( item -> {
						List<String> labelNames = new ArrayList<>();
						List<String> labelValues = new ArrayList<>();
						List<Double> data = new ArrayList<>();
						final Long[] timestamps = new Long[1];
						memberAccessors.forEach((k,v) -> {
							if (v.getMember(item) instanceof IQuantity) {
								labelNames.add(k);
								if (JfrAttributes.END_TIME.getIdentifier().equals(k) ||
									JfrAttributes.START_TIME.getIdentifier().equals(k)){
									try {
										Long value = ((IQuantity)v.getMember(item)).longValueIn(UnitLookup.EPOCH_MS);
										timestamps[0] = value;
										labelValues.add(value.toString());
									} catch (QuantityConversionException e) {
										e.printStackTrace(System.err);
									}
								} else {
									Double value = ((IQuantity)v.getMember(item)).doubleValue();
									labelValues.add(value.toString());
									data.add(value);
								}
							} else if (v.getMember(item) instanceof IMCThread) {
								IMCThread imcThread = (IMCThread) v.getMember(item);
								labelNames.add("threadName");
								labelValues.add(imcThread.getThreadName());
								labelNames.add("threadID");
								labelValues.add(imcThread.getThreadId().toString());
								if (imcThread.getThreadGroup() != null ) {
									labelNames.add("threadGroupName");
									labelValues.add(imcThread.getThreadGroup().getName());
								}

								if (imcThread instanceof JfrThread) {
									JfrThread jfrThread = (JfrThread) imcThread;
									labelNames.add("osThread");
									labelValues.add(Long.toString(((IQuantity)jfrThread.getOsThreadId()).longValue()));
									labelNames.add("osThreadName");
									labelValues.add(jfrThread.getOsName().toString());
								}
							}  else {
								if (null != v && v.getMember(item) != null) {
									labelNames.add(k);
									labelValues.add(v.getMember(item).toString());
								}
							}
						});
						samples.add(new MetricFamilySamples.Sample(metricName, labelNames,labelValues, data.stream().mapToDouble(d -> d).max().orElse(0.0d),
								timestamps[0]));
					});
				});
				metrics.add(new MetricFamilySamples(metricName, metricType, metricDescription, samples));
			} catch(Exception ex) {
				ex.printStackTrace(System.err);
			}
		});
		return metrics;
	}

	static class EmptyRecordingException extends Exception {
		EmptyRecordingException(String msg) {
			super(msg);
		}
	}
}
