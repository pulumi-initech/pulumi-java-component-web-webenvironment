package com.pulumi.components.inputs;

import com.pulumi.core.Output;
import com.pulumi.resources.ResourceArgs;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WebEnvironmentArgs extends ResourceArgs {
	private Output<String> vpcId;

	private Output<String> vpcCidr;

	private Output<String> imageId;

	private Output<Integer> instanceCount;

	private Output<String> instanceType;

	private Output<List<String>> publicSubnetIds;

	private Output<List<String>> privateSubnetIds;

	private Output<String> certificateArn;

	private Output<String> zoneName;

	private Output<String> subdomain;

	private Output<Double> scaleOutRpsThreshold;

	private Output<Double> scaleInRpsThreshold;
}
