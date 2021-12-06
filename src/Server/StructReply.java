package Server;

import Client.MetaData;
import Connection.ConnectionFrame;
import Connection.ReliableConnection;
import Multiplex.ProtocolFrame;

import java.awt.*;
import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StructReply implements Runnable {
    private InetAddress destAdress;
    private int destPort;
    static private String folderToSync;

    public StructReply(InetAddress destAdress, int destPort, String folderToSync) throws SocketException {
        this.folderToSync = folderToSync;
        this.destAdress = destAdress;
        this.destPort = destPort;
    }

    @Override
    public void run() {
        Path path = Paths.get("./" + folderToSync);

        Stream<Path> walk = null;
        try {
            walk = Files.walk(path);
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<Path> paths;
        paths = walk.filter(Files::isRegularFile).collect(Collectors.toList());
        List<MetaData> metaDataPaths = paths.stream().map(p -> getMetaData(p)).toList();

        byte[] data = new byte[0];
        try {
            data = serialize(metaDataPaths);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            ReliableConnection rb = new ReliableConnection(this.destAdress,this.destPort);
            ProtocolFrame pf = new ProtocolFrame((byte)0x3,data.length,data);
            rb.send(pf.serialize());
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /*
    serialize a List of paths
     */
    static byte[] serialize(List <MetaData> metaPaths) throws IOException {
        ByteArrayOutputStream ba = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(ba);

        dos.writeInt(metaPaths.size());

        for (MetaData metaPath : metaPaths){
            dos.write(metaPath.serialize());
        }

        dos.close();
        return ba.toByteArray();
    }

    static List<MetaData> deserialize(byte[] bytes) {
        List<MetaData> metaDataPaths = new ArrayList<>();
        ByteArrayInputStream ba = new ByteArrayInputStream(bytes);
        DataInputStream dos = new DataInputStream(ba);

        try{
            int quant = dos.readInt();
            for(int i = 0; i < quant; i++){
                MetaData metaData = new MetaData();
                metaData.deserialize(dos);
                metaDataPaths.add(metaData);
            }
            dos.close();
        }catch (IOException e){

        }
        return metaDataPaths;
    }

    /*
    Get the MetaData from a path
     */
    private MetaData getMetaData(Path path){
        try {
            BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
            return new MetaData(path.toString(),attr.size(),attr.creationTime().toMillis(),attr.lastModifiedTime().toMillis());
        }catch (IOException e){
        }
        return null;
    }

}
