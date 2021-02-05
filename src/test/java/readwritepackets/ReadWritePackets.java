package readwritepackets;

import com.google.common.reflect.ClassPath;
import net.minestom.server.MinecraftServer;
import net.minestom.server.network.packet.client.ClientPacket;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.utils.binary.BinaryReader;
import net.minestom.server.utils.binary.BinaryWriter;
import net.minestom.server.utils.binary.Readable;
import net.minestom.server.utils.binary.Writeable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * If you are using IntelliJ, set it up so that tests are run through IntelliJ and not Gradle for this collection of tests
 */
public class ReadWritePackets {

    /**
     * Test that all read operations on ServerPacket subclasses are implemented, and use the correct amount of memory.
     * This is by no means a perfect test as some packets have variable length and content has to be validated
     */
    @TestFactory
    public Collection<DynamicTest> checkServerImplementationPresence() throws IOException {
        return checkImplementationPresence(ServerPacket.class);
    }

    /**
     * Test that all write operations on ClientPacket subclasses are implemented, and use the correct amount of memory.
     * This is by no means a perfect test as some packets have variable length and content has to be validated
     */
    @TestFactory
    public Collection<DynamicTest> checkClientImplementationPresence() throws IOException {
        return checkImplementationPresence(ClientPacket.class);
    }

    private <T extends Readable & Writeable> Collection<DynamicTest> checkImplementationPresence(Class<T> packetClass) throws IOException {
        ClassPath cp = ClassPath.from(ClassLoader.getSystemClassLoader());
        List<DynamicTest> allTests = new LinkedList<>();
        for(ClassPath.ClassInfo classInfo : cp.getAllClasses()) {
            if(!classInfo.getPackageName().startsWith("net.minestom.server.network.packet"))
                continue;
            try {
                Class<?> clazz = classInfo.load();
                if(packetClass.isAssignableFrom(clazz) && !clazz.isInterface() && ((clazz.getModifiers() & Modifier.ABSTRACT) != Modifier.ABSTRACT)) {
                    allTests.add(DynamicTest.dynamicTest("WriteThenRead "+clazz.getSimpleName(), () -> {
                        // required for managers to be loaded
                        MinecraftServer.init();

                        BinaryWriter writer = new BinaryWriter();
                        T packet = (T) clazz.getConstructor().newInstance();

                        // write packet
                        packet.write(writer);

                        // re-read packet
                        byte[] originalBytes = writer.toByteArray();
                        BinaryReader reader = new BinaryReader(originalBytes);
                        packet.read(reader);

                        Assertions.assertEquals(0, reader.getRemainingBytes().length, "Packet did not read all available data");

                        // re-write to ensure packet contents are the same
                        BinaryWriter secondWriter = new BinaryWriter();
                        packet.write(secondWriter);

                        // check that contents are the same than before read
                        Assertions.assertArrayEquals(originalBytes, secondWriter.toByteArray());
                    }));
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        }

        return allTests;
    }
}