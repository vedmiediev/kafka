/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package org.apache.kafka.common.record;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@RunWith(value = Parameterized.class)
public class MemoryLogBufferBuilderTest {

    private final CompressionType compressionType;
    private final int bufferOffset;

    public MemoryLogBufferBuilderTest(int bufferOffset, CompressionType compressionType) {
        this.bufferOffset = bufferOffset;
        this.compressionType = compressionType;
    }

    @Test
    public void buildUsingLogAppendTime() {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.position(bufferOffset);

        long logAppendTime = System.currentTimeMillis();
        MemoryLogBufferBuilder builder = new MemoryLogBufferBuilder(buffer, Record.MAGIC_VALUE_V1, compressionType,
                TimestampType.LOG_APPEND_TIME, 0L, logAppendTime, 0, (short) 0, 0, buffer.capacity());
        builder.append(0L, 0L, "a".getBytes(), "1".getBytes());
        builder.append(1L, 0L, "b".getBytes(), "2".getBytes());
        builder.append(2L, 0L, "c".getBytes(), "3".getBytes());
        MemoryLogBuffer logBuffer = builder.build();

        MemoryLogBuffer.RecordsInfo info = builder.info();
        assertEquals(logAppendTime, info.maxTimestamp);

        if (compressionType == CompressionType.NONE)
            assertEquals(0L, info.offsetOfMaxTimestamp);
        else
            assertEquals(2L, info.offsetOfMaxTimestamp);

        for (LogEntry logEntry : logBuffer) {
            assertEquals(TimestampType.LOG_APPEND_TIME, logEntry.timestampType());
            for (LogRecord record : logEntry)
                assertEquals(logAppendTime, record.timestamp());
        }
    }

    @Test
    public void convertUsingLogAppendTime() {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.position(bufferOffset);

        long logAppendTime = System.currentTimeMillis();
        MemoryLogBufferBuilder builder = new MemoryLogBufferBuilder(buffer, Record.MAGIC_VALUE_V1, compressionType,
                TimestampType.LOG_APPEND_TIME, 0L, logAppendTime, 0, (short) 0, 0, buffer.capacity());

        builder.convertAndAppend(0L, Record.create(Record.MAGIC_VALUE_V0, 0L, "a".getBytes(), "1".getBytes()));
        builder.convertAndAppend(1L, Record.create(Record.MAGIC_VALUE_V0, 0L, "b".getBytes(), "2".getBytes()));
        builder.convertAndAppend(2L, Record.create(Record.MAGIC_VALUE_V0, 0L, "c".getBytes(), "3".getBytes()));
        MemoryLogBuffer logBuffer = builder.build();

        MemoryLogBuffer.RecordsInfo info = builder.info();
        assertEquals(logAppendTime, info.maxTimestamp);

        if (compressionType == CompressionType.NONE)
            assertEquals(0L, info.offsetOfMaxTimestamp);
        else
            assertEquals(2L, info.offsetOfMaxTimestamp);

        for (LogEntry logEntry : logBuffer) {
            assertEquals(TimestampType.LOG_APPEND_TIME, logEntry.timestampType());
            for (LogRecord record : logEntry)
                assertEquals(logAppendTime, record.timestamp());
        }
    }

    @Test
    public void buildUsingCreateTime() {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.position(bufferOffset);

        long logAppendTime = System.currentTimeMillis();
        MemoryLogBufferBuilder builder = new MemoryLogBufferBuilder(buffer, Record.MAGIC_VALUE_V1, compressionType,
                TimestampType.CREATE_TIME, 0L, logAppendTime, 0, (short) 0, 0, buffer.capacity());
        builder.append(0L, 0L, "a".getBytes(), "1".getBytes());
        builder.append(1L, 1L, "b".getBytes(), "2".getBytes());
        builder.append(2L, 2L, "c".getBytes(), "3".getBytes());
        MemoryLogBuffer logBuffer = builder.build();

        MemoryLogBuffer.RecordsInfo info = builder.info();
        assertEquals(2L, info.maxTimestamp);
        assertEquals(2L, info.offsetOfMaxTimestamp);

        long i = 0L;
        for (LogEntry logEntry : logBuffer) {
            assertEquals(TimestampType.CREATE_TIME, logEntry.timestampType());
            for (LogRecord record : logEntry)
                assertEquals(i++, record.timestamp());
        }
    }

    @Test
    public void writePastLimit() {
        ByteBuffer buffer = ByteBuffer.allocate(64);
        buffer.position(bufferOffset);

        long logAppendTime = System.currentTimeMillis();
        MemoryLogBufferBuilder builder = new MemoryLogBufferBuilder(buffer, Record.MAGIC_VALUE_V1, compressionType,
                TimestampType.CREATE_TIME, 0L, logAppendTime, 0, (short) 0, 0, buffer.capacity());
        builder.append(0L, 0L, "a".getBytes(), "1".getBytes());
        builder.append(1L, 1L, "b".getBytes(), "2".getBytes());

        assertFalse(builder.hasRoomFor("c".getBytes(), "3".getBytes()));
        builder.append(2L, 2L, "c".getBytes(), "3".getBytes());
        MemoryLogBuffer logBuffer = builder.build();

        MemoryLogBuffer.RecordsInfo info = builder.info();
        assertEquals(2L, info.maxTimestamp);
        assertEquals(2L, info.offsetOfMaxTimestamp);

        long i = 0L;
        for (LogEntry logEntry : logBuffer) {
            assertEquals(TimestampType.CREATE_TIME, logEntry.timestampType());
            for (LogRecord record : logEntry)
                assertEquals(i++, record.timestamp());
        }
    }

    @Test
    public void convertUsingCreateTime() {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.position(bufferOffset);

        long logAppendTime = System.currentTimeMillis();
        MemoryLogBufferBuilder builder = new MemoryLogBufferBuilder(buffer, Record.MAGIC_VALUE_V1, compressionType,
                TimestampType.CREATE_TIME, 0L, logAppendTime, 0, (short) 0, 0, buffer.capacity());

        builder.convertAndAppend(0L, Record.create(Record.MAGIC_VALUE_V0, 0L, "a".getBytes(), "1".getBytes()));
        builder.convertAndAppend(0L, Record.create(Record.MAGIC_VALUE_V0, 0L, "b".getBytes(), "2".getBytes()));
        builder.convertAndAppend(0L, Record.create(Record.MAGIC_VALUE_V0, 0L, "c".getBytes(), "3".getBytes()));
        MemoryLogBuffer logBuffer = builder.build();

        MemoryLogBuffer.RecordsInfo info = builder.info();
        assertEquals(Record.NO_TIMESTAMP, info.maxTimestamp);
        assertEquals(0L, info.offsetOfMaxTimestamp);

        for (LogEntry logEntry : logBuffer) {
            assertEquals(TimestampType.CREATE_TIME, logEntry.timestampType());
            for (LogRecord record : logEntry)
                assertEquals(Record.NO_TIMESTAMP, record.timestamp());
        }
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        List<Object[]> values = new ArrayList<>();
        for (int bufferOffset : Arrays.asList(0, 15))
            for (CompressionType compressionType : CompressionType.values())
                values.add(new Object[] {bufferOffset, compressionType});
        return values;
    }

}