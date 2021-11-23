package org.opendatakit.aggregate.odktables.api;

import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.proxy.auth.AuthType;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.opendatakit.aggregate.odktables.rest.entity.TableDefinition;
import org.opendatakit.aggregate.odktables.rest.entity.TableResource;
import org.opendatakit.aggregate.odktables.rest.entity.TableResourceList;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractServiceTest {

  public static final String TABLE_API = "tables/";
  private static BrowserMobProxy proxy;
  private static int proxyPort;

  private String baseUrl;
  private String appId = "default";
  private URI baseUri;
  protected RestTemplate rt;
  protected HttpHeaders reqHeaders;
  private String tableDefinitionUri = null;

  @BeforeClass
  public static void setupProxy() throws Throwable {
    String hostname = System.getProperty("test.server.hostname");
    String username = System.getProperty("test.server.username");
    String password = System.getProperty("test.server.password");

    try {
      proxy = new BrowserMobProxyServer();
    } catch (Throwable t) {
      t.printStackTrace();
      throw t;
    }
    proxy.start(0);
    proxyPort = proxy.getPort();

    proxy.stopAutoAuthorization(hostname);
    if (username != null && username.length() != 0) {
      proxy.autoAuthorization(hostname, username, password, AuthType.BASIC);
    }
  }
  
  @AfterClass
  public static void teardownProxy() {
    if ( proxy != null ) {
      proxy.stop();
      proxy = null;
      try {
        Thread.sleep(500L);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
  
  // call this from any Before action in derived class
  public void abstractServiceSetUp() throws Exception, Throwable {
    String hostname = System.getProperty("test.server.hostname");
    baseUrl = System.getProperty("test.server.baseUrl", "/");
    String port = System.getProperty("test.server.port");

    this.baseUri = URI.create("http://" + hostname + ":" + port + baseUrl + "odktables/" + appId + "/");

    // RestTemplate
    try {
      this.rt = new RestTemplate();

      SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
      InetSocketAddress address = 
          new InetSocketAddress(proxy.getClientBindAddress().getHostAddress(), proxyPort);

      Proxy proxyRef = new Proxy(Proxy.Type.HTTP, address);
      factory.setProxy(proxyRef);
      factory.setOutputStreaming(false);

      rt.setRequestFactory(factory);
    } catch ( Throwable t ) {
      t.printStackTrace();
      throw t;
    }
    
    this.rt.setErrorHandler(new ErrorHandler());
    List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();

    converters.add(new MappingJackson2HttpMessageConverter());
    // converters.add(new AllEncompassingFormHttpMessageConverter());
    this.rt.setMessageConverters(converters);

    // HttpHeaders
    List<MediaType> acceptableMediaTypes = new ArrayList<MediaType>();
    acceptableMediaTypes.add(MediaType.APPLICATION_JSON_UTF8);

    this.reqHeaders = new HttpHeaders();
    reqHeaders.setAccept(acceptableMediaTypes);
    reqHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
  }
  
  protected URI resolveUri(String str) {
    return baseUri.resolve(str);
  }

  @After
  public void abstractServiceTearDown() throws Exception {
    try {
      if ( tableDefinitionUri != null ) {
        URI uri = resolveUri(tableDefinitionUri);
        this.rt.delete(uri);
      }
    } catch (Exception e) {
      // ignore
      System.out.println(e);
    }
  }

  protected TableResource createTable() throws Throwable  {
    URI uri = resolveUri(TABLE_API + T.tableId);

    TableDefinition definition = new TableDefinition(T.tableId, null, T.columns);
    HttpEntity<TableDefinition> entity = entity(definition);

    ResponseEntity<TableResource> resp;
    try {
      resp = rt.exchange(uri, HttpMethod.PUT, entity, TableResource.class);
    } catch ( Throwable t ) {
      t.printStackTrace();
      throw t;
    }
    TableResource rsc = resp.getBody();
    tableDefinitionUri = rsc.getDefinitionUri();
    return rsc;
  }

  protected TableResource createAltTable() throws Throwable  {
    URI uri = resolveUri(TABLE_API + T.tableId);

    TableDefinition definition = new TableDefinition(T.tableId, null, T.altcolumns);
    HttpEntity<TableDefinition> entity = entity(definition);

    ResponseEntity<TableResource> resp;
    try {
      resp = rt.exchange(uri, HttpMethod.PUT, entity, TableResource.class);
    } catch ( Throwable t ) {
      t.printStackTrace();
      throw t;
    }
    TableResource rsc = resp.getBody();
    tableDefinitionUri = rsc.getDefinitionUri();
    return rsc;
  }

  protected TableResourceList getTables() throws Throwable {
    URI uri = resolveUri(TABLE_API);

    ResponseEntity<TableResourceList> resp;
    try {
      resp = rt.exchange(uri, HttpMethod.GET, null, TableResourceList.class);
    } catch ( Throwable t ) {
      t.printStackTrace();
      throw t;
    }
    TableResourceList trl = resp.getBody();
    return trl;
  }

  protected <V> HttpEntity<V> entity(V entity) {
    return new HttpEntity<V>(entity, reqHeaders);
  }

  private class ErrorHandler implements ResponseErrorHandler {
    @Override
    public void handleError(ClientHttpResponse resp) throws IOException {
      HttpStatus status = resp.getStatusCode();
      String body = readInput(resp.getBody());
      if (status.value() / 100 == 4)
        throw new HttpClientErrorException(status, body);
      else if (status.value() / 100 == 5)
        throw new HttpServerErrorException(status, body);
    }

    @Override
    public boolean hasError(ClientHttpResponse resp) throws IOException {
      return resp.getStatusCode().value() / 100 != 2;
    }

    private String readInput(InputStream is) throws IOException {
      BufferedReader br = new BufferedReader(new InputStreamReader(is));
      StringBuilder sb = new StringBuilder();
      String line;
      while ((line = br.readLine()) != null) {
        sb.append(line);
      }
      return sb.toString();
    }
  }
}
