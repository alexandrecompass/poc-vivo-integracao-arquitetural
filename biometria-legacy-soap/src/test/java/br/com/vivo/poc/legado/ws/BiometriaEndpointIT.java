package br.com.vivo.poc.legado.ws;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.ws.test.server.MockWebServiceClient;
import org.springframework.xml.transform.StringSource;

import javax.xml.transform.Source;
import java.util.Map;

import static org.springframework.ws.test.server.RequestCreators.withPayload;
import static org.springframework.ws.test.server.ResponseMatchers.noFault;
import static org.springframework.ws.test.server.ResponseMatchers.serverOrReceiverFault;
import static org.springframework.ws.test.server.ResponseMatchers.xpath;

@SpringBootTest
class BiometriaEndpointIT {

    private static final Map<String, String> NAMESPACES = Map.of("tns", BiometriaEndpoint.NAMESPACE);

    @Autowired
    private ApplicationContext applicationContext;

    private MockWebServiceClient mockClient;

    @BeforeEach
    void setUp() {
        mockClient = MockWebServiceClient.createClient(applicationContext);
    }

    @Test
    void deveRetornarBiometriaDisponivelParaCpfValido() throws Exception {
        Source request = requestPara("52998224725");

        mockClient.sendRequest(withPayload(request))
                .andExpect(noFault())
                .andExpect(xpath("/tns:ConsultarBiometriaResponse/tns:biometriaDisponivel", NAMESPACES)
                        .evaluatesTo(true))
                .andExpect(xpath("/tns:ConsultarBiometriaResponse/tns:origem", NAMESPACES)
                        .evaluatesTo("LEGADO_SOAP"));
    }

    @Test
    void deveRetornarSoapFaultParaCpfNaoEncontrado() throws Exception {
        Source request = requestPara("00000000000");

        mockClient.sendRequest(withPayload(request))
                .andExpect(serverOrReceiverFault("BIOMETRIA_NAO_ENCONTRADA"));
    }

    @Test
    void deveRetornarSoapFaultQuandoCpfAusenteNoPayload() throws Exception {
        Source request = new StringSource(
                "<tns:ConsultarBiometriaRequest xmlns:tns=\"" + BiometriaEndpoint.NAMESPACE + "\"/>");

        mockClient.sendRequest(withPayload(request))
                .andExpect(serverOrReceiverFault("CPF_AUSENTE"));
    }

    private Source requestPara(String cpf) {
        return new StringSource(
                "<tns:ConsultarBiometriaRequest xmlns:tns=\"" + BiometriaEndpoint.NAMESPACE + "\">"
                        + "<tns:cpf>" + cpf + "</tns:cpf>"
                        + "</tns:ConsultarBiometriaRequest>");
    }
}
