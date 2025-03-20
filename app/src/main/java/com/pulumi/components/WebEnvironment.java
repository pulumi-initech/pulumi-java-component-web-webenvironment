package com.pulumi.components;

import com.pulumi.aws.autoscaling.Attachment;
import com.pulumi.aws.autoscaling.AttachmentArgs;
import com.pulumi.aws.autoscaling.Group;
import com.pulumi.aws.autoscaling.GroupArgs;
import com.pulumi.aws.autoscaling.inputs.GroupLaunchTemplateArgs;
import com.pulumi.aws.ec2.KeyPair;
import com.pulumi.aws.ec2.KeyPairArgs;
import com.pulumi.aws.ec2.LaunchTemplate;
import com.pulumi.aws.ec2.LaunchTemplateArgs;
import com.pulumi.aws.ec2.SecurityGroup;
import com.pulumi.aws.ec2.SecurityGroupArgs;
import com.pulumi.aws.ec2.inputs.SecurityGroupEgressArgs;
import com.pulumi.aws.ec2.inputs.SecurityGroupIngressArgs;
import com.pulumi.aws.route53.Route53Functions;
import com.pulumi.aws.route53.Record;
import com.pulumi.aws.route53.RecordArgs;
import com.pulumi.aws.route53.inputs.GetZoneArgs;
import com.pulumi.aws.route53.inputs.RecordAliasArgs;
import com.pulumi.components.inputs.ApplicationLoadBalancerArgs;
import com.pulumi.components.inputs.RpsAutoscalingPolicyArgs;
import com.pulumi.components.inputs.WebEnvironmentArgs;
import com.pulumi.core.Output;
import com.pulumi.core.annotations.Export;
import com.pulumi.deployment.InvokeOptions;
import com.pulumi.resources.ComponentResource;
import com.pulumi.resources.ComponentResourceOptions;
import com.pulumi.resources.CustomResourceOptions;
import com.pulumi.tls.PrivateKey;
import com.pulumi.tls.PrivateKeyArgs;

import java.util.Base64;
import java.util.List;
import java.util.Map;

public class WebEnvironment extends ComponentResource {

	@Export
	public final Output<String> loadBalancerDnsName;

	@Export
	public final Output<String> fqdn;

	public WebEnvironment(String name, WebEnvironmentArgs args, ComponentResourceOptions options) {
		super("pulumi-components:index:WebEnvironment", name, options);

		var albSg = new SecurityGroup(
				name + "-alb-sg",
				new SecurityGroupArgs.Builder()
						.vpcId(args.getVpcId())
						.ingress(
								new SecurityGroupIngressArgs.Builder()
										.protocol("TCP")
										.fromPort(443)
										.toPort(443)
										.cidrBlocks("0.0.0.0/0")
										.build(),
								new SecurityGroupIngressArgs.Builder()
										.protocol("TCP")
										.fromPort(80)
										.toPort(80)
										.cidrBlocks("0.0.0.0/0")
										.build())
						.egress(
								new SecurityGroupEgressArgs.Builder()
										.protocol("-1")
										.fromPort(0)
										.toPort(0)
										.cidrBlocks("0.0.0.0/0")
										.build())
						.build(),
				CustomResourceOptions.builder()
						.parent(this)
						.protect(true)
						.build());

		var instanceSg = new SecurityGroup(
				name + "-instance-sg",
				new SecurityGroupArgs.Builder()
						.vpcId(args.getVpcId())
						.ingress(
								new SecurityGroupIngressArgs.Builder()
										.protocol("TCP")
										.fromPort(22)
										.toPort(22)
										.cidrBlocks(args.getVpcCidr().applyValue(c -> List.of(c)))
										.build(),
								new SecurityGroupIngressArgs.Builder()
										.protocol("TCP")
										.fromPort(80)
										.toPort(80)
										.securityGroups(Output.all(albSg.id()))
										.build())
						.egress(
								new SecurityGroupEgressArgs.Builder()
										.protocol("-1")
										.fromPort(0)
										.toPort(0)
										.cidrBlocks("0.0.0.0/0")
										.build())
						.build(),
				CustomResourceOptions.builder().parent(this).build());

		var material = new PrivateKey(
				name + "-private-key",
				new PrivateKeyArgs.Builder().algorithm("RSA").build(),
				CustomResourceOptions.builder().parent(this).build());

		var sshKey = new KeyPair(
				name + "-key-pair",
				new KeyPairArgs.Builder().publicKey(material.publicKeyOpenssh()).build(),
				CustomResourceOptions.builder().parent(material).build());

		var launchTemplate = new LaunchTemplate(
				name + "-launch-config",
				new LaunchTemplateArgs.Builder()
						.namePrefix(name + "-web")
						.instanceType(args.getInstanceType())
						.imageId(args.getImageId())
						.keyName(sshKey.keyName())
						.vpcSecurityGroupIds(Output.all(instanceSg.id()))
						.tags(Map.of("Role", "Webserver"))
						.userData(
								new String(
										Base64.getEncoder()
												.encode(
														"""
																#!/bin/bash
																sudo yum update -y
																sudo amazon-linux-extras install nginx1 -y
																sudo systemctl enable nginx
																sudo systemctl start nginx"""
																.getBytes())))
						.build(),
				CustomResourceOptions.builder().parent(this).build());

		var asg = new Group(
				name + "-asg",
				new GroupArgs.Builder()
						.vpcZoneIdentifiers(args.getPublicSubnetIds())
						.desiredCapacity(1)
						.maxSize(1)
						.minSize(1)
						.launchTemplate(
								new GroupLaunchTemplateArgs.Builder()
										.id(launchTemplate.id())
										.version("$Latest")
										.build())
						.build(),
				CustomResourceOptions.builder().parent(launchTemplate).build());

		new RpsAutoscalingPolicy(
				name + "rps-scaling-policy",
				RpsAutoscalingPolicyArgs.builder()
						.autoScalingGroupName(asg.name())
						.highRequestThreshold(args.getScaleOutRpsThreshold())
						.lowRequestThreshold(args.getScaleInRpsThreshold())
						.build(),
				ComponentResourceOptions.builder().parent(asg).build());

		var loadBalancer = new ApplicationLoadBlancer(
				name + "-lb",
				ApplicationLoadBalancerArgs.builder()
						.securityGroupIds(Output.all(albSg.id()))
						.subnetIds(args.getPublicSubnetIds())
						.vpcId(args.getVpcId())
						.certificateArn(args.getCertificateArn())
						.build(),
				ComponentResourceOptions.builder().parent(asg).build());

		new Attachment(
				name + "-alb-att",
				new AttachmentArgs.Builder()
						.autoscalingGroupName(asg.name())
						.lbTargetGroupArn(loadBalancer.targetGroupId)
						.build(),
				CustomResourceOptions.builder().parent(this).build());

		var zoneResult = Route53Functions.getZone(
				GetZoneArgs.builder()
						.name(args.getZoneName())
						.privateZone(false)
						.build(),
				InvokeOptions.builder().parent(this).build());

		var record = new Record(
				name + "-alb-alias-record",
				new RecordArgs.Builder()
						.zoneId(zoneResult.applyValue(r -> r.zoneId()))
						.name(args.getSubdomain())
						.type("A")
						.aliases(
								new RecordAliasArgs.Builder()
										.name(loadBalancer.dnsName)
										.zoneId(loadBalancer.hostedZoneId)
										.evaluateTargetHealth(true)
										.build())
						.build(),
				CustomResourceOptions.builder().parent(this).build());

		this.fqdn = record.fqdn();
		this.loadBalancerDnsName = loadBalancer.dnsName;
	}
}
