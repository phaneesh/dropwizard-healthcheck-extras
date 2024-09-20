package io.dyuti.dropwizard.healtcheck;

import com.codahale.metrics.health.HealthCheck;
import io.dropwizard.setup.Environment;
import io.dyuti.dropwizard.config.MetricHealthCheckConfig;
import lombok.RequiredArgsConstructor;

/**
 * Health Check for Log Rate
 */
@RequiredArgsConstructor
public class MetricHealthCheck extends HealthCheck {

  private final Environment environment;
  private final MetricHealthCheckConfig config;

  @Override
  protected Result check() {
    return switch(config.getType()) {
      case METER -> checkMeter();
      case TIMER -> checkTimer();
      case HISTOGRAM -> checkHistogram();
      case COUNTER -> checkCounter();
      case GAUGE -> checkGauge();
    };
  }

  private Result checkMeter() {
    if(environment.metrics().getMeters().containsKey(config.getMetric())) {
      var meter = environment.metrics().getMeters().get(config.getMetric());
      var value = switch(config.getDimension()) {
        case "m5_rate" -> meter.getFiveMinuteRate();
        case "m15_rate" -> meter.getFifteenMinuteRate();
        case "mean_rate" -> meter.getMeanRate();
        case "count" -> meter.getCount();
        default -> meter.getOneMinuteRate();
      };
      if(value > config.getThreshold()) {
        return Result.unhealthy("Meter " +config.getMetric() +"[" +config.getDimension() +"]" +"  exceeded threshold: " + value);
      }
    }
    return Result.healthy();
  }

  private Result checkTimer() {
    if(environment.metrics().getTimers().containsKey(config.getMetric())) {
      var timer = environment.metrics().getTimers().get(config.getMetric());
      var value = switch(config.getDimension()) {
        case "max" -> timer.getSnapshot().getMax();
        case "min" -> timer.getSnapshot().getMin();
        case "mean" -> timer.getSnapshot().getMean();
        case "stddev" -> timer.getSnapshot().getStdDev();
        case "p75" -> timer.getSnapshot().get75thPercentile();
        case "p95" -> timer.getSnapshot().get95thPercentile();
        case "p98" -> timer.getSnapshot().get98thPercentile();
        case "p99" -> timer.getSnapshot().get99thPercentile();
        case "p999" -> timer.getSnapshot().get999thPercentile();
        case "median" -> timer.getSnapshot().getMedian();
        default -> -1;
      };
      if(value > 0 && value > config.getThreshold()) {
        return Result.unhealthy("Timer " +config.getMetric() +"[" +config.getDimension() +"]" +"  exceeded threshold: " + value);
      }
    }
    return Result.healthy();
  }

  private Result checkHistogram() {
    if(environment.metrics().getHistograms().containsKey(config.getMetric())) {
      var histogram = environment.metrics().getHistograms().get(config.getMetric());
      var value = switch(config.getDimension()) {
        case "max" -> histogram.getSnapshot().getMax();
        case "min" -> histogram.getSnapshot().getMin();
        case "mean" -> histogram.getSnapshot().getMean();
        case "stddev" -> histogram.getSnapshot().getStdDev();
        case "p75" -> histogram.getSnapshot().get75thPercentile();
        case "p95" -> histogram.getSnapshot().get95thPercentile();
        case "p98" -> histogram.getSnapshot().get98thPercentile();
        case "p99" -> histogram.getSnapshot().get99thPercentile();
        case "p999" -> histogram.getSnapshot().get999thPercentile();
        case "median" -> histogram.getSnapshot().getMedian();
        default -> -1;
      };
      if(value > 0 && value > config.getThreshold()) {
        return Result.unhealthy("Histogram " +config.getMetric() +"[" +config.getDimension() +"]" +"  exceeded threshold: " + value);
      }
    }
    return Result.healthy();
  }

  private Result checkCounter() {
    if(environment.metrics().getCounters().containsKey(config.getMetric())) {
      var counter = environment.metrics().getCounters().get(config.getMetric());
      if(counter.getCount() > config.getThreshold()) {
        return Result.unhealthy("Counter " +config.getMetric() +"[" +config.getDimension() +"]" +"  exceeded threshold: " + counter.getCount());
      }
    }
    return Result.healthy();
  }

  private Result checkGauge() {
    if(environment.metrics().getGauges().containsKey(config.getMetric())) {
      var gauge = environment.metrics().getGauges().get(config.getMetric());
      if(gauge.getValue() instanceof Number) {
        var value = ((Number) gauge.getValue()).longValue();
        if(value > config.getThreshold()) {
          return Result.unhealthy("Gauge " +config.getMetric() +"[" +config.getDimension() +"]" +"  exceeded threshold: " + value);
        }
      }
    }
    return Result.healthy();
  }
}
