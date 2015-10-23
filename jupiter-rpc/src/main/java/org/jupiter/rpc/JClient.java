package org.jupiter.rpc;

import org.jupiter.registry.NotifyListener;
import org.jupiter.registry.RegisterMeta;
import org.jupiter.registry.Registry;
import org.jupiter.rpc.channel.JChannel;
import org.jupiter.rpc.channel.JChannelGroup;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public interface JClient extends Registry {

    JChannelGroup group(UnresolvedAddress address);

    Collection<JChannelGroup> groups();

    void addGroup(Directory directory, JChannelGroup group);

    CopyOnWriteArrayList<JChannelGroup> directory(Directory directory);

    JChannel select(Directory directory);

    Collection<RegisterMeta> lookup(Directory directory);

    void subscribe(Directory directory, NotifyListener listener);
}