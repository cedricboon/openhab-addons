package org.openhab.binding.velbus.handler.internal;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.openhab.binding.velbus.internal.VelbusBindingConstants.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.junit.Before;
import org.junit.Test;
import org.openhab.binding.velbus.internal.handler.VelbusVMBGPOHandler;

public class VelbusVMBGPOMemoTextTest extends AbstractVelbusThingTest {
    private static final String TEST_MODULE_ADDRESS = "01";

    private Map<String, Object> thingProperties;
    private Thing vmbgpoThing;
    private VelbusVMBGPOHandler velbusVMBGPOHandler;

    private final ThingUID THING_UID_VMBGPO = new ThingUID(THING_TYPE_VMBGPO, "testvmbgpo");
    private final ChannelUID CHANNEL_MEMO_TEXT = new ChannelUID(THING_UID_VMBGPO, "oledDisplay#MEMO");
    private final ChannelTypeUID TEXT_CHANNEL_TYPEUID = new ChannelTypeUID("velbus", "text");

    private final byte[] GET_STATUS_PACKET = new byte[] { 0x0F, (byte) 0xFB, 0x01, 0x02, (byte) 0xFA, (byte) 0xFF,
            (byte) 0xFA, 0x04 };

    @Before
    public void setUp() {
        super.setup();

        thingProperties = new HashMap<>();
        thingProperties.put(ADDRESS, TEST_MODULE_ADDRESS);
        vmbgpoThing = ThingBuilder.create(THING_TYPE_VMBGPO, "testvmbgpo").withLabel("Vmbgpo Thing")
                .withBridge(bridge.getUID()).withConfiguration(new Configuration(thingProperties))
                .withChannel(ChannelBuilder.create(CHANNEL_MEMO_TEXT, "Dimmer").withType(TEXT_CHANNEL_TYPEUID).build())
                .build();

        velbusVMBGPOHandler = new VelbusVMBGPOHandler(vmbgpoThing) {
            @Override
            protected @Nullable Bridge getBridge() {
                return bridge;
            }
        };
        initializeHandler(velbusVMBGPOHandler);
    }

    @Test
    public void testSendMemoTextCommand() {
        velbusVMBGPOHandler.handleCommand(CHANNEL_MEMO_TEXT, new StringType("This is a demo text..."));

        byte[] memoPacket1 = new byte[] { 0x0F, (byte) 0xFB, 0x01, 0x08, (byte) 0xAC, 0x00, 0x00, 0x54, 0x68, 0x69,
                0x73, 0x20, (byte) 0x89, 0x04 };
        byte[] memoPacket2 = new byte[] { 0x0F, (byte) 0xFB, 0x01, 0x08, (byte) 0xAC, 0x00, 0x05, 0x69, 0x73, 0x20,
                0x61, 0x20, (byte) 0xBF, 0x04 };
        byte[] memoPacket3 = new byte[] { 0x0F, (byte) 0xFB, 0x01, 0x08, (byte) 0xAC, 0x00, 0x0A, 0x64, 0x65, 0x6D,
                0x6F, 0x20, 0x72, 0x04 };
        byte[] memoPacket4 = new byte[] { 0x0F, (byte) 0xFB, 0x01, 0x08, (byte) 0xAC, 0x00, 0x0F, 0x74, 0x65, 0x78,
                0x74, 0x2E, 0x3F, 0x04 };
        byte[] memoPacket5 = new byte[] { 0x0F, (byte) 0xFB, 0x01, 0x08, (byte) 0xAC, 0x00, 0x14, 0x2E, 0x2E, 0x00,
                0x00, 0x00, (byte) 0xD1, 0x04 };

        List<byte[]> packets = getBridgePackets(6);
        assertThat(packets.get(0), equalTo(GET_STATUS_PACKET));
        assertThat(packets.get(1), equalTo(memoPacket1));
        assertThat(packets.get(2), equalTo(memoPacket2));
        assertThat(packets.get(3), equalTo(memoPacket3));
        assertThat(packets.get(4), equalTo(memoPacket4));
        assertThat(packets.get(5), equalTo(memoPacket5));
    }

    @Test
    public void testSendMemoTextCommandWith5characters() {
        velbusVMBGPOHandler.handleCommand(CHANNEL_MEMO_TEXT, new StringType("abcde"));

        byte[] memoPacket1 = new byte[] { 0x0F, (byte) 0xFB, 0x01, 0x08, (byte) 0xAC, 0x00, 0x00, 0x61, 0x62, 0x63,
                0x64, 0x65, (byte) 0x52, 0x04 };
        byte[] memoPacket2 = new byte[] { 0x0F, (byte) 0xFB, 0x01, 0x08, (byte) 0xAC, 0x00, 0x05, 0x00, 0x00, 0x00,
                0x00, 0x00, (byte) 0x3C, 0x04 };

        List<byte[]> packets = getBridgePackets(3);
        assertThat(packets.get(0), equalTo(GET_STATUS_PACKET));
        assertThat(packets.get(1), equalTo(memoPacket1));
        assertThat(packets.get(2), equalTo(memoPacket2));
    }

    @Test
    public void testSendMemoTextCommandWith10characters() {
        velbusVMBGPOHandler.handleCommand(CHANNEL_MEMO_TEXT, new StringType("abcdefghij"));

        byte[] memoPacket1 = new byte[] { 0x0F, (byte) 0xFB, 0x01, 0x08, (byte) 0xAC, 0x00, 0x00, 0x61, 0x62, 0x63,
                0x64, 0x65, (byte) 0x52, 0x04 };
        byte[] memoPacket2 = new byte[] { 0x0F, (byte) 0xFB, 0x01, 0x08, (byte) 0xAC, 0x00, 0x05, 0x66, 0x67, 0x68,
                0x69, 0x6A, (byte) 0x34, 0x04 };
        byte[] memoPacket3 = new byte[] { 0x0F, (byte) 0xFB, 0x01, 0x08, (byte) 0xAC, 0x00, 0x0A, 0x00, 0x00, 0x00,
                0x00, 0x00, (byte) 0x37, 0x04 };

        List<byte[]> packets = getBridgePackets(4);
        assertThat(packets.get(0), equalTo(GET_STATUS_PACKET));
        assertThat(packets.get(1), equalTo(memoPacket1));
        assertThat(packets.get(2), equalTo(memoPacket2));
        assertThat(packets.get(3), equalTo(memoPacket3));
    }

    @Test
    public void testSendMemoTextCommandWith64characters() {
        velbusVMBGPOHandler.handleCommand(CHANNEL_MEMO_TEXT,
                new StringType("abcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcd"));

        byte[] memoPacket1 = new byte[] { 0x0F, (byte) 0xFB, 0x01, 0x08, (byte) 0xAC, 0x00, 0x00, 0x61, 0x62, 0x63,
                0x64, 0x65, (byte) 0x52, 0x04 };
        byte[] memoPacket2 = new byte[] { 0x0F, (byte) 0xFB, 0x01, 0x08, (byte) 0xAC, 0x00, 0x05, 0x66, 0x67, 0x68,
                0x69, 0x6A, (byte) 0x34, 0x04 };
        byte[] memoPacket3 = new byte[] { 0x0F, (byte) 0xFB, 0x01, 0x08, (byte) 0xAC, 0x00, 0x0A, 0x61, 0x62, 0x63,
                0x64, 0x65, (byte) 0x48, 0x04 };
        byte[] memoPacket4 = new byte[] { 0x0F, (byte) 0xFB, 0x01, 0x08, (byte) 0xAC, 0x00, 0x0F, 0x66, 0x67, 0x68,
                0x69, 0x6A, (byte) 0x2A, 0x04 };
        byte[] memoPacket5 = new byte[] { 0x0F, (byte) 0xFB, 0x01, 0x08, (byte) 0xAC, 0x00, 0x14, 0x61, 0x62, 0x63,
                0x64, 0x65, (byte) 0x3E, 0x04 };
        byte[] memoPacket6 = new byte[] { 0x0F, (byte) 0xFB, 0x01, 0x08, (byte) 0xAC, 0x00, 0x19, 0x66, 0x67, 0x68,
                0x69, 0x6A, (byte) 0x20, 0x04 };
        byte[] memoPacket7 = new byte[] { 0x0F, (byte) 0xFB, 0x01, 0x08, (byte) 0xAC, 0x00, 0x1E, 0x61, 0x62, 0x63,
                0x64, 0x65, (byte) 0x34, 0x04 };
        byte[] memoPacket8 = new byte[] { 0x0F, (byte) 0xFB, 0x01, 0x08, (byte) 0xAC, 0x00, 0x23, 0x66, 0x67, 0x68,
                0x69, 0x6A, (byte) 0x16, 0x04 };
        byte[] memoPacket9 = new byte[] { 0x0F, (byte) 0xFB, 0x01, 0x08, (byte) 0xAC, 0x00, 0x28, 0x61, 0x62, 0x63,
                0x64, 0x65, (byte) 0x2A, 0x04 };
        byte[] memoPacket10 = new byte[] { 0x0F, (byte) 0xFB, 0x01, 0x08, (byte) 0xAC, 0x00, 0x2D, 0x66, 0x67, 0x68,
                0x69, 0x6A, (byte) 0x0C, 0x04 };
        byte[] memoPacket11 = new byte[] { 0x0F, (byte) 0xFB, 0x01, 0x08, (byte) 0xAC, 0x00, 0x32, 0x61, 0x62, 0x63,
                0x64, 0x65, (byte) 0x20, 0x04 };
        byte[] memoPacket12 = new byte[] { 0x0F, (byte) 0xFB, 0x01, 0x08, (byte) 0xAC, 0x00, 0x37, 0x66, 0x67, 0x68,
                0x69, 0x6A, (byte) 0x02, 0x04 };
        byte[] memoPacket13 = new byte[] { 0x0F, (byte) 0xFB, 0x01, 0x08, (byte) 0xAC, 0x00, 0x3C, 0x61, 0x62, 0x63,
                0x00, 0x00, (byte) 0xDF, 0x04 };

        List<byte[]> packets = getBridgePackets(14);
        assertThat(packets.get(0), equalTo(GET_STATUS_PACKET));
        assertThat(packets.get(1), equalTo(memoPacket1));
        assertThat(packets.get(2), equalTo(memoPacket2));
        assertThat(packets.get(3), equalTo(memoPacket3));
        assertThat(packets.get(4), equalTo(memoPacket4));
        assertThat(packets.get(5), equalTo(memoPacket5));
        assertThat(packets.get(6), equalTo(memoPacket6));
        assertThat(packets.get(7), equalTo(memoPacket7));
        assertThat(packets.get(8), equalTo(memoPacket8));
        assertThat(packets.get(9), equalTo(memoPacket9));
        assertThat(packets.get(10), equalTo(memoPacket10));
        assertThat(packets.get(11), equalTo(memoPacket11));
        assertThat(packets.get(12), equalTo(memoPacket12));
        assertThat(packets.get(13), equalTo(memoPacket13));
    }

    @Test
    public void testSendMemoTextCommandWithMoreThan64characters() {
        velbusVMBGPOHandler.handleCommand(CHANNEL_MEMO_TEXT,
                new StringType("abcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghij"));

        byte[] memoPacket1 = new byte[] { 0x0F, (byte) 0xFB, 0x01, 0x08, (byte) 0xAC, 0x00, 0x00, 0x61, 0x62, 0x63,
                0x64, 0x65, (byte) 0x52, 0x04 };
        byte[] memoPacket2 = new byte[] { 0x0F, (byte) 0xFB, 0x01, 0x08, (byte) 0xAC, 0x00, 0x05, 0x66, 0x67, 0x68,
                0x69, 0x6A, (byte) 0x34, 0x04 };
        byte[] memoPacket3 = new byte[] { 0x0F, (byte) 0xFB, 0x01, 0x08, (byte) 0xAC, 0x00, 0x0A, 0x61, 0x62, 0x63,
                0x64, 0x65, (byte) 0x48, 0x04 };
        byte[] memoPacket4 = new byte[] { 0x0F, (byte) 0xFB, 0x01, 0x08, (byte) 0xAC, 0x00, 0x0F, 0x66, 0x67, 0x68,
                0x69, 0x6A, (byte) 0x2A, 0x04 };
        byte[] memoPacket5 = new byte[] { 0x0F, (byte) 0xFB, 0x01, 0x08, (byte) 0xAC, 0x00, 0x14, 0x61, 0x62, 0x63,
                0x64, 0x65, (byte) 0x3E, 0x04 };
        byte[] memoPacket6 = new byte[] { 0x0F, (byte) 0xFB, 0x01, 0x08, (byte) 0xAC, 0x00, 0x19, 0x66, 0x67, 0x68,
                0x69, 0x6A, (byte) 0x20, 0x04 };
        byte[] memoPacket7 = new byte[] { 0x0F, (byte) 0xFB, 0x01, 0x08, (byte) 0xAC, 0x00, 0x1E, 0x61, 0x62, 0x63,
                0x64, 0x65, (byte) 0x34, 0x04 };
        byte[] memoPacket8 = new byte[] { 0x0F, (byte) 0xFB, 0x01, 0x08, (byte) 0xAC, 0x00, 0x23, 0x66, 0x67, 0x68,
                0x69, 0x6A, (byte) 0x16, 0x04 };
        byte[] memoPacket9 = new byte[] { 0x0F, (byte) 0xFB, 0x01, 0x08, (byte) 0xAC, 0x00, 0x28, 0x61, 0x62, 0x63,
                0x64, 0x65, (byte) 0x2A, 0x04 };
        byte[] memoPacket10 = new byte[] { 0x0F, (byte) 0xFB, 0x01, 0x08, (byte) 0xAC, 0x00, 0x2D, 0x66, 0x67, 0x68,
                0x69, 0x6A, (byte) 0x0C, 0x04 };
        byte[] memoPacket11 = new byte[] { 0x0F, (byte) 0xFB, 0x01, 0x08, (byte) 0xAC, 0x00, 0x32, 0x61, 0x62, 0x63,
                0x64, 0x65, (byte) 0x20, 0x04 };
        byte[] memoPacket12 = new byte[] { 0x0F, (byte) 0xFB, 0x01, 0x08, (byte) 0xAC, 0x00, 0x37, 0x66, 0x67, 0x68,
                0x69, 0x6A, (byte) 0x02, 0x04 };
        byte[] memoPacket13 = new byte[] { 0x0F, (byte) 0xFB, 0x01, 0x08, (byte) 0xAC, 0x00, 0x3C, 0x61, 0x62, 0x63,
                0x00, 0x00, (byte) 0xDF, 0x04 };

        List<byte[]> packets = getBridgePackets(14);
        assertThat(packets.get(0), equalTo(GET_STATUS_PACKET));
        assertThat(packets.get(1), equalTo(memoPacket1));
        assertThat(packets.get(2), equalTo(memoPacket2));
        assertThat(packets.get(3), equalTo(memoPacket3));
        assertThat(packets.get(4), equalTo(memoPacket4));
        assertThat(packets.get(5), equalTo(memoPacket5));
        assertThat(packets.get(6), equalTo(memoPacket6));
        assertThat(packets.get(7), equalTo(memoPacket7));
        assertThat(packets.get(8), equalTo(memoPacket8));
        assertThat(packets.get(9), equalTo(memoPacket9));
        assertThat(packets.get(10), equalTo(memoPacket10));
        assertThat(packets.get(11), equalTo(memoPacket11));
        assertThat(packets.get(12), equalTo(memoPacket12));
        assertThat(packets.get(13), equalTo(memoPacket13));
    }
}
