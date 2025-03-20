package com.pulumi.components.inputs;

import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import com.pulumi.core.Output;
import com.pulumi.resources.ResourceArgs;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RpsAutoscalingPolicyArgs extends ResourceArgs {
    private Output<String> autoScalingGroupName;
    private Output<Double> highRequestThreshold;
    private Output<Double> lowRequestThreshold;
}
