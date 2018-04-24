package org.openhab.binding.velbus.handler;

import static org.mockito.Mockito.*;

import java.util.List;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerCallback;
import org.eclipse.smarthome.test.java.JavaTest;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class AbstractVelbusThingTest extends JavaTest {
    protected Bridge bridge;
    protected VelbusBridgeHandler velbusBridgeHandler;
    protected ThingHandlerCallback mockCallback;

    protected void setup() {
        initializeBridge();

        mockCallback = mock(ThingHandlerCallback.class);
    }

    private void initializeBridge() {
        bridge = Mockito.mock(Bridge.class);
        when(bridge.getStatus()).thenReturn(ThingStatus.ONLINE);

        velbusBridgeHandler = Mockito.mock(VelbusBridgeHandler.class);
        when(bridge.getHandler()).thenReturn(velbusBridgeHandler);

        bridge.setHandler(velbusBridgeHandler);
        ThingHandlerCallback bridgeHandler = mock(ThingHandlerCallback.class);

        velbusBridgeHandler.setCallback(bridgeHandler);
        velbusBridgeHandler.initialize();
    }

    protected void initializeHandler(ThingHandler handler) {
        handler.getThing().setHandler(handler);
        handler.setCallback(mockCallback);
        handler.initialize();
    }

    protected List<byte[]> getBridgePackets(int numberOfPackets) {
        ArgumentCaptor<byte[]> argumentCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(velbusBridgeHandler, times(numberOfPackets)).sendPacket(argumentCaptor.capture());

        return argumentCaptor.getAllValues();
    }
}
