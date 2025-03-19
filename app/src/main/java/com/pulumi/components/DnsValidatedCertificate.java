package com.pulumi.components;

import com.pulumi.aws.acm.Certificate;
import com.pulumi.aws.acm.CertificateArgs;
import com.pulumi.aws.acm.CertificateValidation;
import com.pulumi.aws.acm.CertificateValidationArgs;
import com.pulumi.aws.route53.Record;
import com.pulumi.aws.route53.RecordArgs;
import com.pulumi.aws.route53.Route53Functions;
import com.pulumi.aws.route53.inputs.GetZoneArgs;
import com.pulumi.components.inputs.DnsValidatedCertificateArgs;
import com.pulumi.core.annotations.Export;
import com.pulumi.deployment.InvokeOptions;
import com.pulumi.core.Either;
import com.pulumi.core.Output;
import com.pulumi.resources.ComponentResource;
import com.pulumi.resources.ComponentResourceOptions;
import com.pulumi.resources.CustomResourceOptions;
import java.util.List;

public class DnsValidatedCertificate extends ComponentResource {

	@Export
	public final Output<String> certificateArn;

	@Export
	public final Output<String> zoneId;

	@Export
	public final Output<List<String>> zoneValidationFqdns;

	public DnsValidatedCertificate(String name, DnsValidatedCertificateArgs args, ComponentResourceOptions options) {
		super("pulumi-components:index:DnsValidatedCertificate", name, options);

		var cert = new Certificate(
				name + "-cert",
				new CertificateArgs.Builder().domainName(args.getDomainName()).validationMethod("DNS").build(),
				CustomResourceOptions.builder().parent(this).build());

		var zoneResult = Route53Functions.getZone(GetZoneArgs.builder().name(args.getZoneName()).build(), InvokeOptions.builder().parent(this).build());

		this.zoneId = zoneResult.applyValue(z -> z.zoneId());

		Record certValidationRecord = new Record(
				name + "-cert-validation-record",
				new RecordArgs.Builder()
						.name(cert.domainValidationOptions()
								.applyValue(o -> o.getFirst().resourceRecordName().get()))
						.records(cert.domainValidationOptions()
								.applyValue(o -> List.of(o.getFirst().resourceRecordValue().get())))
						.type(cert.domainValidationOptions()
								.applyValue(o -> Either.ofLeft(o.getFirst().resourceRecordType().get())))
						.zoneId(this.zoneId)
						.ttl(60)
						.build(),
				CustomResourceOptions.builder().parent(this).build());

		this.zoneValidationFqdns = certValidationRecord.fqdn().applyValue(fqdn -> List.of(fqdn));

		var certCertifcateValidation = new CertificateValidation(
				name + "-cert-validatiton",
				new CertificateValidationArgs.Builder()
						.certificateArn(cert.arn())
						.validationRecordFqdns(zoneValidationFqdns)
						.build(),
				CustomResourceOptions.builder().parent(cert).build());

		this.certificateArn = certCertifcateValidation.certificateArn();
	}
}
