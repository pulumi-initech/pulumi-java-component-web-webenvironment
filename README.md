# Pulumi Java Web Environment Components

A collection of reusable Pulumi components for deploying scalable web hosting environments on AWS using Java.

## Overview

This repository provides four main component resources that work together to create a complete, production-ready web hosting platform:

- **WebEnvironment**: Complete web hosting environment with load balancing, auto-scaling, and DNS
- **ApplicationLoadBalancer**: HTTPS-enabled load balancer with SSL termination
- **DnsValidatedCertificate**: Auto-validated SSL certificates via Route53
- **RpsAutoscalingPolicy**: Request-based auto scaling for dynamic capacity management

## Components

### WebEnvironment

**Type:** `pulumi-components:index:WebEnvironment`

The main orchestrator component that creates a complete web hosting environment.

**Features:**
- Auto Scaling Group with Launch Template for web servers
- Security groups with appropriate ingress/egress rules
- SSH key pair generation for EC2 access
- Nginx web server installation via user data
- Integration with Application Load Balancer
- Route53 DNS alias record configuration

**Outputs:**
- `loadBalancerDnsName`: DNS name of the load balancer
- `fqdn`: Fully qualified domain name

### ApplicationLoadBalancer

**Type:** `pulumi-components:index:ApplicationLoadBlancer`

Internet-facing Application Load Balancer with HTTPS termination and HTTP redirect.

**Features:**
- Cross-zone load balancing enabled
- HTTPS listener (port 443) with SSL certificate
- HTTP listener (port 80) with 301 redirect to HTTPS
- Target group for HTTP traffic on port 80

**Outputs:**
- `dnsName`: Load balancer DNS name
- `targetGroupId`: Target group identifier
- `hostedZoneId`: Route53 hosted zone ID

### DnsValidatedCertificate

**Type:** `pulumi-components:index:DnsValidatedCertificate`

AWS Certificate Manager SSL certificate with automatic DNS validation.

**Features:**
- ACM certificate creation for specified domain
- Automatic Route53 validation record creation
- DNS-based validation workflow
- Certificate validation completion waiting

**Outputs:**
- `certificateArn`: ARN of the validated certificate
- `zoneId`: Route53 zone identifier
- `zoneValidationFqdns`: Validation FQDNs

### RpsAutoscalingPolicy

**Type:** `pulumi-components:index:RpsAutoscalingPolicy`

Auto scaling policies based on Application Load Balancer request metrics.

**Features:**
- Scale-out policy (+1 instance) for high traffic
- Scale-in policy (-1 instance) for low traffic
- CloudWatch alarms monitoring ALB RequestCount metric
- Configurable request thresholds
- 180-second evaluation periods and cooldown

## Prerequisites

- Java 23 or later
- Gradle
- Pulumi CLI
- AWS credentials configured

## Usage

### Building the Components

```bash
./gradlew build
```

### Using in Pulumi Programs

After building and running the component provider, you can use these components in your Pulumi programs:


```java
// Example usage of WebEnvironment component
var webEnv = new WebEnvironment("my-web-env", WebEnvironmentArgs.builder()
    .vpcId("vpc-12345")
    .subnetIds(List.of("subnet-1", "subnet-2"))
    .domainName("example.com")
    .zoneName("example.com")
    .instanceType("t3.micro")
    .minSize(1)
    .maxSize(5)
    .highRequestThreshold(1000)
    .lowRequestThreshold(100)
    .build());
```

## Architecture

The components follow a hierarchical architecture:

```
WebEnvironment (Main Orchestrator)
├── ApplicationLoadBalancer
│   └── DnsValidatedCertificate
└── RpsAutoscalingPolicy
```

## Dependencies

- Pulumi Java SDK v1.6.0
- AWS SDK v6.66.3
- TLS Provider v5.0.0
- Lombok for code generation

## Development

The project uses Gradle with Java 23 and follows the component provider pattern with `ComponentProviderHost`.

**Project Structure:**
- `app/src/main/java/com/pulumi/components/` - Component implementations
- `app/src/main/java/com/pulumi/components/inputs/` - Input argument classes
- `app/src/main/java/com/pulumi/components/App.java` - Main provider entry point
