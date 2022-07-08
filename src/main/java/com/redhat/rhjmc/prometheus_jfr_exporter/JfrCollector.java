package com.redhat.rhjmc.prometheus_jfr_exporter;

import io.prometheus.client.Collector;
import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.item.*;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.rjmx.services.jfr.FlightRecorderException;

import java.io.IOException;
import java.io.InputStream;
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
						TreeMap<String,String> labels = new TreeMap<>();
						List<String> labelNames = new ArrayList<>();
						List<String> labelValues = new ArrayList<>();
						List<Double> data = new ArrayList<>();
						final Long[] timestamps = new Long[1];
						memberAccessors.forEach((k,v) -> {
							if (v.getMember(item) instanceof IQuantity) {
								if (JfrAttributes.END_TIME.getIdentifier().equals(k) ||
									JfrAttributes.START_TIME.getIdentifier().equals(k)){
									try {
										Long value = ((IQuantity)v.getMember(item)).longValueIn(UnitLookup.EPOCH_MS);
										timestamps[0] = value;
										labels.put(k,value.toString());
									} catch (QuantityConversionException e) {
										e.printStackTrace(System.err);
									}
								} else {
									Double value = ((IQuantity)v.getMember(item)).doubleValue();
									labels.put(k,value.toString());
									data.add(value);
								}
							} else if (v.getMember(item) instanceof IMCThread) {
								IMCThread imcThread = (IMCThread) v.getMember(item);
								labels.put("threadName", imcThread.getThreadName());
								labels.put("threadID", imcThread.getThreadId().toString());
								if (imcThread.getThreadGroup() != null ) {
									labels.put("threadGroupName",imcThread.getThreadGroup().getName());
								}
							}  else {
								if (null != v && v.getMember(item) != null) {
									labels.put(k,v.getMember(item).toString());
								}
							}
						});
						labels.forEach((k,v) -> {
							labelNames.add(k);
							labelValues.add(v);
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
