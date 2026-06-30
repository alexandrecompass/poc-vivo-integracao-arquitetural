package br.com.vivo.poc.biometria.adapter.soap;

import br.com.vivo.poc.biometria.domain.Biometria;
import br.com.vivo.poc.biometria.domain.Cpf;
import br.com.vivo.poc.biometria.domain.OrigemBiometria;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.transform.dom.DOMResult;
import java.time.LocalDateTime;

@Component
public class SoapBiometriaMapper {

    public Biometria toDomain(Object result, Cpf cpf) {
        Element response = extractResponseElement(result);
        return new Biometria(
                cpf,
                Boolean.parseBoolean(getChildValue(response, "biometriaDisponivel")),
                getChildValue(response, "imagemBase64"),
                OrigemBiometria.valueOf(getChildValue(response, "origem")),
                LocalDateTime.parse(getChildValue(response, "dataConsulta"))
        );
    }

    private Element extractResponseElement(Object result) {
        if (!(result instanceof DOMResult domResult)) {
            throw new IllegalArgumentException("Resultado SOAP invalido.");
        }
        Node node = domResult.getNode();
        if (node instanceof Document document) {
            return document.getDocumentElement();
        }
        if (node instanceof Element element) {
            return element;
        }
        throw new IllegalArgumentException("Nao foi possivel extrair resposta SOAP.");
    }

    private String getChildValue(Element parent, String localName) {
        NodeList nodes = parent.getElementsByTagNameNS("*", localName);
        if (nodes.getLength() == 0) {
            return null;
        }
        return nodes.item(0).getTextContent();
    }
}
