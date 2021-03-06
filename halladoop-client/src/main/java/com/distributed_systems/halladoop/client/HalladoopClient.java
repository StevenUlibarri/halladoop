package com.distributed_systems.halladoop.client;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.distributed_systems.halladoop.dataNode.model.ReadWorker;
import com.distributed_systems.halladoop.dataNode.model.WriteWorker;

/**
 * Created by devin on 12/3/15.
 */
public class HalladoopClient {
    public static final int BLOCK_SIZE = 64 * 1048576;
    private static final int cores = Runtime.getRuntime().availableProcessors();

    private final String NAME_NODE_ADDRESS;
    private final int PORT;
    private final ExecutorService workers;

    public HalladoopClient(String nameNodeAddress, int port) {
        this.PORT = port;
        this.NAME_NODE_ADDRESS = nameNodeAddress;
        this.workers = Executors.newFixedThreadPool(cores);
    }

    public void write(String fileName, File file) {
        WriteWorker writeWorker = new WriteWorker(fileName, file, NAME_NODE_ADDRESS, PORT);
        workers.execute(writeWorker);
    }

    public File read(String fileName, String destination) throws ExecutionException, InterruptedException {
        ReadWorker readWorker = new ReadWorker(fileName, NAME_NODE_ADDRESS, PORT, destination);
        Future<File> file = workers.submit(readWorker);

        return file.get();
    }

    private void delete(String fileName) {

    }
}