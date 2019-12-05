/*
 * Copyright (c) 2008-2019, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.client.impl.protocol.codec;

import com.hazelcast.client.impl.protocol.ClientMessage;
import com.hazelcast.client.impl.protocol.Generated;
import com.hazelcast.client.impl.protocol.codec.builtin.*;
import com.hazelcast.client.impl.protocol.codec.custom.*;

import javax.annotation.Nullable;

import static com.hazelcast.client.impl.protocol.ClientMessage.*;
import static com.hazelcast.client.impl.protocol.codec.builtin.FixedSizeTypesCodec.*;

/*
 * This file is auto-generated by the Hazelcast Client Protocol Code Generator.
 * To change this file, edit the templates or the protocol
 * definitions on the https://github.com/hazelcast/hazelcast-client-protocol
 * and regenerate it.
 */

/**
 * Initiate WAN sync for a specific map or all maps
 */
@Generated("e9fe61b5ed5b5cf9c7f99b241a8e2b5f")
public final class MCWanSyncMapCodec {
    //hex: 0x201600
    public static final int REQUEST_MESSAGE_TYPE = 2102784;
    //hex: 0x201601
    public static final int RESPONSE_MESSAGE_TYPE = 2102785;
    private static final int REQUEST_WAN_SYNC_TYPE_FIELD_OFFSET = PARTITION_ID_FIELD_OFFSET + INT_SIZE_IN_BYTES;
    private static final int REQUEST_INITIAL_FRAME_SIZE = REQUEST_WAN_SYNC_TYPE_FIELD_OFFSET + INT_SIZE_IN_BYTES;
    private static final int RESPONSE_UUID_FIELD_OFFSET = RESPONSE_BACKUP_ACKS_FIELD_OFFSET + INT_SIZE_IN_BYTES;
    private static final int RESPONSE_INITIAL_FRAME_SIZE = RESPONSE_UUID_FIELD_OFFSET + UUID_SIZE_IN_BYTES;

    private MCWanSyncMapCodec() {
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings({"URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD"})
    public static class RequestParameters {

        /**
         * Name of the WAN replication to initiate WAN sync for
         */
        public java.lang.String wanReplicationName;

        /**
         * ID of the WAN publisher to initiate WAN sync for
         */
        public java.lang.String wanPublisherId;

        /**
         * Whether all maps are going to be synced or only a single one:
         * 0 - ALL_MAPS
         * 1 - SINGLE_MAP
         */
        public int wanSyncType;

        /**
         * Name of the map to trigger WAN sync on, null if all maps are to be synced
         */
        public @Nullable java.lang.String mapName;
    }

    public static ClientMessage encodeRequest(java.lang.String wanReplicationName, java.lang.String wanPublisherId, int wanSyncType, @Nullable java.lang.String mapName) {
        ClientMessage clientMessage = ClientMessage.createForEncode();
        clientMessage.setRetryable(false);
        clientMessage.setOperationName("MC.WanSyncMap");
        ClientMessage.Frame initialFrame = new ClientMessage.Frame(new byte[REQUEST_INITIAL_FRAME_SIZE], UNFRAGMENTED_MESSAGE);
        encodeInt(initialFrame.content, TYPE_FIELD_OFFSET, REQUEST_MESSAGE_TYPE);
        encodeInt(initialFrame.content, REQUEST_WAN_SYNC_TYPE_FIELD_OFFSET, wanSyncType);
        clientMessage.add(initialFrame);
        StringCodec.encode(clientMessage, wanReplicationName);
        StringCodec.encode(clientMessage, wanPublisherId);
        CodecUtil.encodeNullable(clientMessage, mapName, StringCodec::encode);
        return clientMessage;
    }

    public static MCWanSyncMapCodec.RequestParameters decodeRequest(ClientMessage clientMessage) {
        ClientMessage.ForwardFrameIterator iterator = clientMessage.frameIterator();
        RequestParameters request = new RequestParameters();
        ClientMessage.Frame initialFrame = iterator.next();
        request.wanSyncType = decodeInt(initialFrame.content, REQUEST_WAN_SYNC_TYPE_FIELD_OFFSET);
        request.wanReplicationName = StringCodec.decode(iterator);
        request.wanPublisherId = StringCodec.decode(iterator);
        request.mapName = CodecUtil.decodeNullable(iterator, StringCodec::decode);
        return request;
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings({"URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD"})
    public static class ResponseParameters {

        /**
         * UUID of the synchronization
         */
        public java.util.UUID uuid;
    }

    public static ClientMessage encodeResponse(java.util.UUID uuid) {
        ClientMessage clientMessage = ClientMessage.createForEncode();
        ClientMessage.Frame initialFrame = new ClientMessage.Frame(new byte[RESPONSE_INITIAL_FRAME_SIZE], UNFRAGMENTED_MESSAGE);
        encodeInt(initialFrame.content, TYPE_FIELD_OFFSET, RESPONSE_MESSAGE_TYPE);
        encodeUUID(initialFrame.content, RESPONSE_UUID_FIELD_OFFSET, uuid);
        clientMessage.add(initialFrame);

        return clientMessage;
    }

    public static MCWanSyncMapCodec.ResponseParameters decodeResponse(ClientMessage clientMessage) {
        ClientMessage.ForwardFrameIterator iterator = clientMessage.frameIterator();
        ResponseParameters response = new ResponseParameters();
        ClientMessage.Frame initialFrame = iterator.next();
        response.uuid = decodeUUID(initialFrame.content, RESPONSE_UUID_FIELD_OFFSET);
        return response;
    }

}
