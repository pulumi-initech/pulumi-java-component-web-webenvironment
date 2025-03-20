package com.pulumi.components;

import java.util.List;
import java.util.Map;

import com.pulumi.aws.autoscaling.Policy;
import com.pulumi.aws.autoscaling.PolicyArgs;
import com.pulumi.aws.cloudwatch.MetricAlarm;
import com.pulumi.aws.cloudwatch.MetricAlarmArgs;
import com.pulumi.components.inputs.RpsAutoscalingPolicyArgs;
import com.pulumi.resources.ComponentResource;
import com.pulumi.resources.ComponentResourceOptions;
import com.pulumi.resources.CustomResourceOptions;

public class RpsAutoscalingPolicy extends ComponentResource {
	public RpsAutoscalingPolicy(String name, RpsAutoscalingPolicyArgs args, ComponentResourceOptions options) {
		super("pulumi-components:index:RpsAutoscalingPolicy", name, options);

		var scaleOutPolicy = new Policy(name + "-scale-out-olicy", PolicyArgs.builder()
				.adjustmentType("ChangeInCapacity")
				.scalingAdjustment(1)
				.autoscalingGroupName(args.getAutoScalingGroupName())
				.build(),
				CustomResourceOptions.builder().parent(this).build());

		var scaleInPolicy = new Policy(name + "-scale-in-policy", PolicyArgs.builder()
				.adjustmentType("ChangeInCapacity")
				.scalingAdjustment(-1)
				.cooldown(180)
				.autoscalingGroupName(args.getAutoScalingGroupName())
				.build(),
				CustomResourceOptions.builder().parent(this).build());

	   new MetricAlarm(name + "-alb-high-request-alarm", MetricAlarmArgs.builder()
				.namespace("AWS/ApplicationELB")
				.metricName("RequestCount")
				.dimensions(Map.of("LoadBalancer", name + "-alb"))
				.period(180)
				.evaluationPeriods(2)
				.threshold(args.getHighRequestThreshold())
				.comparisonOperator("GreaterThanOrEqualToThreshold")
				.statistic("Sum")
				.alarmActions(scaleOutPolicy.arn().applyValue(arn -> List.of(arn)))
				.build(),
				CustomResourceOptions.builder().parent(scaleOutPolicy).build());

		new MetricAlarm(name + "-alb-low-request-alarm", MetricAlarmArgs.builder()
				.namespace("AWS/ApplicationELB")
				.metricName("RequestCount")
				.dimensions(Map.of("LoadBalancer", name + "-alb"))
				.period(180)
				.evaluationPeriods(2)
				.threshold(args.getLowRequestThreshold())
				.comparisonOperator("LessThanOrEqualToThreshold")
				.statistic("Sum")
				.alarmActions(scaleInPolicy.arn().applyValue(arn -> List.of(arn)))
				.build(),
				CustomResourceOptions.builder().parent(scaleInPolicy).build());
	}
}
