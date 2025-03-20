package com.pulumi.components;

import com.pulumi.aws.alb.Listener;
import com.pulumi.aws.alb.ListenerArgs;
import com.pulumi.aws.alb.LoadBalancer;
import com.pulumi.aws.alb.LoadBalancerArgs;
import com.pulumi.aws.alb.TargetGroup;
import com.pulumi.aws.alb.TargetGroupArgs;
import com.pulumi.aws.alb.inputs.ListenerDefaultActionArgs;
import com.pulumi.aws.alb.inputs.ListenerDefaultActionRedirectArgs;
import com.pulumi.components.inputs.ApplicationLoadBalancerArgs;
import com.pulumi.core.Output;
import com.pulumi.core.annotations.Export;
import com.pulumi.resources.ComponentResource;
import com.pulumi.resources.ComponentResourceOptions;
import com.pulumi.resources.CustomResourceOptions;

public class ApplicationLoadBlancer extends ComponentResource {

	@Export
	public final Output<String> dnsName;

	@Export
	public final Output<String> targetGroupId;

	@Export
	public final Output<String> hostedZoneId;

    public ApplicationLoadBlancer(String name, ApplicationLoadBalancerArgs args, ComponentResourceOptions options) {
		super("pulumi-components:index:ApplicationLoadBlancer", name, options);

		var alb = new LoadBalancer(
				name + "-alb",
				new LoadBalancerArgs.Builder()
						.internal(false)
						.enableCrossZoneLoadBalancing(true)
						.securityGroups(args.getSecurityGroupIds())
						.subnets(args.getSubnetIds())
						.build(),
				CustomResourceOptions.builder().parent(this).build());

		this.dnsName = alb.dnsName();
		this.hostedZoneId = alb.zoneId();

		var tg = new TargetGroup(
				name + "-tg",
				new TargetGroupArgs.Builder()
						.targetType("instance")
						.port(80)
						.protocol("HTTP")
						.vpcId(args.getVpcId())
						.build(),
				CustomResourceOptions.builder().parent(alb).build());

		this.targetGroupId = tg.id();

		new Listener(
				name + "-frontend-https",
				new ListenerArgs.Builder()
						.loadBalancerArn(alb.arn())
						.port(443)
						.protocol("HTTPS")
						.certificateArn(args.getCertificateArn())
						.defaultActions(
								new ListenerDefaultActionArgs.Builder()
										.type("forward")
										.targetGroupArn(tg.arn())
										.build())
						.build(),
				CustomResourceOptions.builder().parent(alb).build());

		new Listener(
				name + "-frontend-redir",
				new ListenerArgs.Builder()
						.loadBalancerArn(alb.arn())
						.port(80)
						.protocol("HTTP")
						.defaultActions(
								new ListenerDefaultActionArgs.Builder()
										.type("redirect")
										.redirect(
												new ListenerDefaultActionRedirectArgs.Builder()
														.protocol("HTTPS")
														.port("443")
														.statusCode("HTTP_301")
														.build())
										.build())
						.build(),
				CustomResourceOptions.builder().parent(alb).build());


	}
}
