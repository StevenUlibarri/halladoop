package com.distributed_systems.halladoop.dataNode.model;

import static com.distributed_systems.halladoop.client.utils.FileUtils.createBlocks;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.codehaus.jackson.map.ObjectMapper;

import spark.utils.IOUtils;

import com.distributed_systems.halladoop.client.data.name_node.Node;
import com.distributed_systems.halladoop.client.data.name_node.NodeWrapper;

/**
 * Created by devin on 12/8/15.
 */
public class WriteWorker implements Runnable {
    private static final String WRITE = "/write/";
    private static final String FINALIZE = "/finalize/" ;
    private static final int DATA_NODE_PORT = 4567;

    private final File file;
    private final String fileName;
    private final String host;
    private final int port;

    public WriteWorker(String fileName, File file, String host, int port) {
        this.fileName = fileName;
        this.file = file;
        this.host = host;
        this.port = port;
    }

    @Override
    public void run() {
        ObjectMapper mapper = new ObjectMapper();
        CloseableHttpClient client = HttpClients.createDefault();
        URIBuilder uriBuilder = new URIBuilder();

        try {
            URI uri = uriBuilder.setScheme("http").setHost(host).setPort(port).setPath(WRITE).build();
            HttpPost writePayload = new HttpPost(uri);
            writePayload.addHeader("Content-Type", "application/json");

            List<WriteData> blocks = createBlocks(file);

            String a  ="{" +
                    "\"file_path\": \"/" + this.fileName + "\","
                    + "\"num_blocks\": \"" + blocks.size()
                    + "\"}";
            StringEntity entity = new StringEntity(a);

            writePayload.setEntity(entity);
            CloseableHttpResponse response = client.execute(writePayload);
            int responseCode = response.getStatusLine().getStatusCode();

            if (responseCode >= 200 && responseCode < 300) {
                Node[] dataNodes = mapper.readValue(response.getEntity().getContent(), NodeWrapper.class).getNodes();

                boolean dataNodeFinalize = true;
                Iterator<WriteData> blockIterator = blocks.iterator();

                while (blockIterator.hasNext() && dataNodeFinalize) {
                    WriteData block = blockIterator.next();
                    List<Integer> nodes = new ArrayList<>();

                    for (Node node : dataNodes) {
                        nodes.add(node.getNode_id());
                        Socket connection = new Socket(node.getNode_ip(), DATA_NODE_PORT);
                        ObjectOutputStream outputStream = new ObjectOutputStream(connection.getOutputStream());

                        outputStream.writeObject(Operation.WRITE);
                        outputStream.flush();
                        outputStream.writeObject(block);
                        outputStream.flush();
                        ObjectInputStream inputStream = new ObjectInputStream(connection.getInputStream());


                        try {
                            dataNodeFinalize = (Boolean) inputStream.readObject();
                        } catch (ClassNotFoundException e) {
                            dataNodeFinalize = false;
                        }

                        connection.close();
                    }

                    if (dataNodeFinalize) {
                    	ObjectMapper mapper2 =  new ObjectMapper();
                    	String data = mapper2.writeValueAsString(new FinalizeInfo(block.getBlockId(), nodes));
                        entity = new StringEntity(data);
                        entity.setContentType("application/json");
                        uri = uriBuilder.setHost(host).setPort(port).setPath(FINALIZE).build();
                        writePayload = new HttpPost(uri);
                        writePayload.setEntity(entity);
                        HttpResponse response2 = client.execute(writePayload);
            			StringWriter writer = new StringWriter();
            			IOUtils.copy(response2.getEntity().getContent(), writer);
            			String theString = writer.toString();
            			System.out.println(theString);
                    }
                }
            } else {
            	StringWriter writer = new StringWriter();
    			IOUtils.copy(response.getEntity().getContent(), writer);
    			String theString = writer.toString();
    			System.out.println(theString);
                throw new WriteException("Server responded with: " + response);
            }
        } catch (IOException e) {
        	e.printStackTrace();
            throw new WriteException("The file " + file.getName()
                    + " was not written due to: \n" + e.getMessage());
        } catch (URISyntaxException e) {
            throw new WriteException("The file " + file.getName()
                    + " was not written due to: \n" + e.getMessage());
        }
    }
}
