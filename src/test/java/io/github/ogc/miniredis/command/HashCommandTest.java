package io.github.ogc.miniredis.command;

import io.github.ogc.miniredis.core.RedisDb;
import io.github.ogc.miniredis.core.object.RedisHash;
import io.github.ogc.miniredis.core.object.RedisString;
import io.github.ogc.miniredis.resp.RespArray;
import io.github.ogc.miniredis.resp.RespBulkString;
import io.github.ogc.miniredis.resp.RespInteger;
import io.github.ogc.miniredis.resp.RespObject;
import io.github.ogc.miniredis.resp.RespSimpleString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HashCommandTest {

    private final RedisDb db = RedisDb.getInstance();
    private final HsetCommand hset = new HsetCommand();
    private final HgetCommand hget = new HgetCommand();
    private final HdelCommand hdel = new HdelCommand();
    private final HgetallCommand hgetall = new HgetallCommand();
    private final HlenCommand hlen = new HlenCommand();

    @BeforeEach
    void setUp() {
        db.clear();
    }

    // ==================== HSET ====================

    @Test
    void hset_newKey_createsHashAndReturnsOne() {
        var r = hset.execute(db, array("HSET", "k", "f1", "v1"));
        assertThat(r).isEqualTo(new RespInteger(1));

        var obj = db.get("k");
        assertThat(obj).isInstanceOf(RedisHash.class);
        assertThat(((RedisHash) obj).getFields()).containsEntry("f1", "v1");
    }

    @Test
    void hset_overwriteField_returnsZero() {
        hset.execute(db, array("HSET", "k", "f1", "v1"));
        var r = hset.execute(db, array("HSET", "k", "f1", "v2"));
        assertThat(r).isEqualTo(new RespInteger(0));  // 覆盖,不计数
    }

    @Test
    void hset_wrongType_returnsWrongType() {
        db.put("k", new RedisString("not-a-hash"));
        var r = hset.execute(db, array("HSET", "k", "f1", "v1"));
        assertThat(((RespSimpleString) r).getValue()).startsWith("WRONGTYPE");
    }

    // ==================== HGET ====================

    @Test
    void hget_noKey_returnsNull() {
        var r = hget.execute(db, array("HGET", "no-key", "f1"));
        assertThat(r).isEqualTo(RespBulkString.NULL);
    }

    @Test
    void hget_existingField_returnsValue() {
        hset.execute(db, array("HSET", "k", "name", "alice"));
        var r = hget.execute(db, array("HGET", "k", "name"));
        assertThat(r).isEqualTo(new RespBulkString("alice"));
    }

    @Test
    void hget_missingField_returnsNull() {
        hset.execute(db, array("HSET", "k", "name", "alice"));
        var r = hget.execute(db, array("HGET", "k", "age"));
        assertThat(r).isEqualTo(RespBulkString.NULL);
    }

    @Test
    void hget_wrongType_returnsWrongType() {
        db.put("k", new RedisString("no"));
        var r = hget.execute(db, array("HGET", "k", "f1"));
        assertThat(((RespSimpleString) r).getValue()).startsWith("WRONGTYPE");
    }

    // ==================== HDEL ====================

    @Test
    void hdel_noKey_returnsZero() {
        var r = hdel.execute(db, array("HDEL", "no-key", "f1"));
        assertThat(r).isEqualTo(new RespInteger(0));
    }

    @Test
    void hdel_singleField_removesAndReturnsOne() {
        hset.execute(db, array("HSET", "k", "f1", "v1"));
        hset.execute(db, array("HSET", "k", "f2", "v2"));
        var r = hdel.execute(db, array("HDEL", "k", "f1"));
        assertThat(r).isEqualTo(new RespInteger(1));
        assertThat(hget.execute(db, array("HGET", "k", "f1")))
                .isEqualTo(RespBulkString.NULL);
        assertThat(hget.execute(db, array("HGET", "k", "f2")))
                .isEqualTo(new RespBulkString("v2"));
    }

    @Test
    void hdel_multiFields_returnsCount() {
        hset.execute(db, array("HSET", "k", "f1", "v1"));
        hset.execute(db, array("HSET", "k", "f2", "v2"));
        hset.execute(db, array("HSET", "k", "f3", "v3"));
        var r = hdel.execute(db, array("HDEL", "k", "f1", "f3", "no-such"));
        assertThat(r).isEqualTo(new RespInteger(2));
    }

    @Test
    void hdel_wrongType_returnsWrongType() {
        db.put("k", new RedisString("no"));
        var r = hdel.execute(db, array("HDEL", "k", "f1"));
        assertThat(((RespSimpleString) r).getValue()).startsWith("WRONGTYPE");
    }

    // ==================== HGETALL ====================

    @Test
    void hgetall_noKey_returnsEmptyArray() {
        var r = hgetall.execute(db, array("HGETALL", "no-key"));
        assertThat(r).isEqualTo(new RespArray(List.of()));
    }

    @Test
    void hgetall_withFields_returnsAlternatingPairs() {
        hset.execute(db, array("HSET", "k", "name", "alice"));
        hset.execute(db, array("HSET", "k", "age", "25"));
        var r = (RespArray) hgetall.execute(db, array("HGETALL", "k"));

        assertThat(r.size()).isEqualTo(4);
        // field/value 交替出现
        assertThat(((RespBulkString) r.get(0)).getValue()).isEqualTo("name");
        assertThat(((RespBulkString) r.get(1)).getValue()).isEqualTo("alice");
        assertThat(((RespBulkString) r.get(2)).getValue()).isEqualTo("age");
        assertThat(((RespBulkString) r.get(3)).getValue()).isEqualTo("25");
    }

    @Test
    void hgetall_wrongType_returnsWrongType() {
        db.put("k", new RedisString("no"));
        var r = hgetall.execute(db, array("HGETALL", "k"));
        assertThat(((RespSimpleString) r).getValue()).startsWith("WRONGTYPE");
    }

    // ==================== HLEN ====================

    @Test
    void hlen_noKey_returnsZero() {
        var r = hlen.execute(db, array("HLEN", "no-key"));
        assertThat(r).isEqualTo(new RespInteger(0));
    }

    @Test
    void hlen_withFields_returnsCount() {
        hset.execute(db, array("HSET", "k", "f1", "v1"));
        hset.execute(db, array("HSET", "k", "f2", "v2"));
        hset.execute(db, array("HSET", "k", "f3", "v3"));
        var r = hlen.execute(db, array("HLEN", "k"));
        assertThat(r).isEqualTo(new RespInteger(3));
    }

    @Test
    void hlen_wrongType_returnsWrongType() {
        db.put("k", new RedisString("no"));
        var r = hlen.execute(db, array("HLEN", "k"));
        assertThat(((RespSimpleString) r).getValue()).startsWith("WRONGTYPE");
    }

    // ==================== helper ====================

    private static RespArray array(String cmd, String... args) {
        RespBulkString[] elements = new RespBulkString[1 + args.length];
        elements[0] = new RespBulkString(cmd);
        for (int i = 0; i < args.length; i++) {
            elements[i + 1] = new RespBulkString(args[i]);
        }
        return new RespArray(List.of(elements));
    }
}
