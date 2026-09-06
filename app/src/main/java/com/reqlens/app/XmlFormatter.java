package com.reqlens.app;
import java.io.*;import javax.xml.parsers.*;import javax.xml.transform.*;import javax.xml.transform.dom.DOMSource;import javax.xml.transform.stream.StreamResult;import org.xml.sax.InputSource;
public final class XmlFormatter{
 private XmlFormatter(){}
 public static String pretty(String xml)throws Exception{DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);f.setExpandEntityReferences(false);DocumentBuilder b=f.newDocumentBuilder();org.w3c.dom.Document d=b.parse(new InputSource(new StringReader(xml)));Transformer t=TransformerFactory.newInstance().newTransformer();t.setOutputProperty(OutputKeys.INDENT,"yes");t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount","2");StringWriter w=new StringWriter();t.transform(new DOMSource(d),new StreamResult(w));return w.toString();}
}
