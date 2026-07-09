package com.usainsrht.elytratrails.skins;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Locale;

/**
 * Encodes SkinsRestorer proxy plugin messages for backend → Velocity communication.
 * Protocol: {@code SRProxyPluginMessage} with a {@code setSkin} GUI action.
 */
final class SkinRestorerMessages {

    static final String MESSAGE_CHANNEL = "sr:messagechannel";

    private SkinRestorerMessages() {
    }

    static byte[] createSetSkinPayload(String skinName) {
        try {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(byteOut);

            // SRProxyPluginMessage.ChannelType.GUI_ACTION_LIST
            out.writeUTF("guiActionList");

            // GUIActionListChannelPayload — single setSkin action
            out.writeInt(1);
            out.writeUTF("setSkin");

            // SetSkinPayload → SkinIdentifier (proxy runs: skin set "<identifier>")
            out.writeUTF(skinName.toLowerCase(Locale.ROOT));
            out.writeBoolean(false); // no SkinVariant
            out.writeUTF("custom");  // SkinType.CUSTOM

            return byteOut.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to encode SkinsRestorer plugin message", e);
        }
    }
}
