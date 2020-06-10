package com.redhat.idaas.connect.builder;

import com.redhat.idaas.connect.configuration.EndpointUriBuilder;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.component.kafka.KafkaConstants;
import com.redhat.idaas.connect.processor.KafkaToNATSProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * Defines a FHIR R4 REST Processing route
 */
public class FhirR4RestRouteBuilder extends IdaasRouteBuilder {

    private final Logger logger = LoggerFactory.getLogger(FhirR4RestRouteBuilder.class);

    @Override
    public void configure() {
        EndpointUriBuilder uriBuilder = getEndpointUriBuilder();
        URI fhirBaseUri = URI.create(uriBuilder.getFhirR4RestUri());
        String kafkaDataStoreUri = uriBuilder.getDataStoreUri("FHIR_R4_${headers.resourceType.toUpperCase()}");
        String messagingUri = uriBuilder.getMessagingUri();
        Processor kafkaToNATS = new KafkaToNATSProcessor();

        restConfiguration()
                .host(fhirBaseUri.getHost())
                .port(fhirBaseUri.getPort());

        rest(fhirBaseUri.getPath())
                .post("/{resource}")
                .route()
                .routeId("fhir-r4-rest")
                .unmarshal().fhirJson("R4")
                .setHeader("resourceType",simple("${body.resourceType.toString()}"))
                .marshal().fhirJson("R4")
                .setHeader(KafkaConstants.KEY, constant("Camel"))
                .doTry()
                    .toD(kafkaDataStoreUri)
                    .process(kafkaToNATS)
                    .to(messagingUri)
                .doCatch(Exception.class)
                   .setBody(exceptionMessage())
                   .log(LoggingLevel.ERROR, logger, "${body}")
                .end();
    }
}
