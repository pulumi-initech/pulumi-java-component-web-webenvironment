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
public class ApplicationLoadBalancerArgs extends ResourceArgs {

	private Output<String> vpcId;

	private Output<String> vpcCidr;

	private Output<List<String>> subnetIds;

	private Output<String> certificateArn;

	private Output<List<String>> securityGroupIds;

}
