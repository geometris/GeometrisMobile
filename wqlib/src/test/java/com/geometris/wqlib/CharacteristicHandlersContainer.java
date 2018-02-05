package com.geometris.wqlib;

import android.os.Handler;

import java.util.HashMap;
import java.util.UUID;

/**
 * Maps service and characteristics, as given by identifiers, to a Handler.
 * @see Handler
 *
 */
public class CharacteristicHandlersContainer {
    private HashMap<UUID, HashMap<UUID, Handler>> mHandlers = new HashMap<UUID, HashMap<UUID, Handler>>();

    /**
     * Add a new characteristic handler.
     *
     * @param service
     *            The UUID of the service that contains the characteristic to register a handler for.
     * @param characteristic
     *            The UUID of the characteristic to register a handler for.
     * @param notifyHandler
     *            The Handler itself.
     */
    public void addHandler(UUID service, UUID characteristic, Handler notifyHandler) {
        HashMap<UUID, Handler> subMap = mHandlers.get(service);
        if (subMap == null) {
            subMap = new HashMap<UUID, Handler>();
            mHandlers.put(service, subMap);
        }
        subMap.put(characteristic, notifyHandler);
    }

    /**
     * Remove a handler from the map.
     *
     * @param service
     *            The UUID of the service that contains the characteristic to remove.
     * @param characteristic
     *            The UUID of the characteristic to remove.
     */
    public void removeHandler(UUID service, UUID characteristic) {
        HashMap<UUID, Handler> subMap = mHandlers.get(service);
        if (subMap != null) {
            subMap.remove(characteristic);
        }
    }

    /**
     * Retrieve a handler.
     *
     * @param service
     *            The UUID of the service that contains the characteristic of interest.
     * @param characteristic
     *            The UUID of the characteristic who's handler should be returned.
     * @return The Handler object.
     */
    public Handler getHandler(UUID service, UUID characteristic) {
        HashMap<UUID, Handler> subMap = mHandlers.get(service);
        if (subMap == null) {
            return null;
        }
        return subMap.get(characteristic);
    }

}
