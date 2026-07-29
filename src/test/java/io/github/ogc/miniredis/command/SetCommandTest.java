package io.github.ogc.miniredis.command;

import io.github.ogc.miniredis.core.RedisDb;
import io.github.ogc.miniredis.core.object.RedisHash;
import io.github.ogc.miniredis.resp.RespArray;
import io.github.ogc.miniredis.resp.RespBulkString;
import io.github.ogc.miniredis.resp.RespInteger;
import io.github.ogc.miniredis.resp.RespSimpleString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SetCommandTest {

    private final RedisDb db = RedisDb.getInstance();
    private final SaddCommand sadd = new SaddCommand();
    private final SremCommand srem = new SremCommand();
    private final SmembersCommand smembers = new SmembersCommand();
    private final SismemberCommand sismember = new SismemberCommand();

    @BeforeEach
    void setUp() {
        db.clear();
    }

    // ==================== SADD ====================

    @Test
    void sadd_newKey_createsSetAndAddsMember() {
        var r = sadd.execute(db, array("SADD", "k", "m1"));
        assertThat(r).isEqualTo(new RespInteger(1));
    }

    @Test
    void sadd_duplicate_returnsZero() {
        sadd.execute(db, array("SADD", "k", "m1"));
        var r = sadd.execute(db, array("SADD", "k", "m1"));
        assertThat(r).isEqualTo(new RespInteger(0));
    }

    @Test
    void sadd_multiMembers_returnsNewCount() {
        sadd.execute(db, array("SADD", "k", "m1"));
        // m1 已在, m2/m3 是新的
        var r = sadd.execute(db, array("SADD", "k", "m1", "m2", "m3"));
        assertThat(r).isEqualTo(new RespInteger(2));
    }

    @Test
    void sadd_wrongType_returnsWrongType() {
        db.put("k", new RedisHash());
        var r = sadd.execute(db, array("SADD", "k", "m1"));
        assertThat(((RespSimpleString) r).getValue()).startsWith("WRONGTYPE");
    }

    // ==================== SREM ====================

    @Test
    void srem_noKey_returnsZero() {
        var r = srem.execute(db, array("SREM", "no-key", "m1"));
        assertThat(r).isEqualTo(new RespInteger(0));
    }

    @Test
    void srem_normal_removesMember() {
        sadd.execute(db, array("SADD", "k", "m1", "m2"));
        var r = srem.execute(db, array("SREM", "k", "m1"));
        assertThat(r).isEqualTo(new RespInteger(1));
        assertThat(sismember.execute(db, array("SISMEMBER", "k", "m1")))
                .isEqualTo(new RespInteger(0));
    }

    @Test
    void srem_wrongType_returnsWrongType() {
        db.put("k", new RedisHash());
        var r = srem.execute(db, array("SREM", "k", "m1"));
        assertThat(((RespSimpleString) r).getValue()).startsWith("WRONGTYPE");
    }

    // ==================== SMEMBERS ====================

    @Test
    void smembers_noKey_returnsEmpty() {
        var r = (RespArray) smembers.execute(db, array("SMEMBERS", "no-key"));
        assertThat(r.size()).isEqualTo(0);
    }

    @Test
    void smembers_normal_returnsAllMembers() {
        sadd.execute(db, array("SADD", "k", "alice", "bob"));
        var r = (RespArray) smembers.execute(db, array("SMEMBERS", "k"));
        assertThat(r.size()).isEqualTo(2);
    }

    @Test
    void smembers_wrongType_returnsWrongType() {
        db.put("k", new RedisHash());
        var r = smembers.execute(db, array("SMEMBERS", "k"));
        assertThat(((RespSimpleString) r).getValue()).startsWith("WRONGTYPE");
    }

    // ==================== SISMEMBER ====================

    @Test
    void sismember_noKey_returnsZero() {
        var r = sismember.execute(db, array("SISMEMBER", "no-key", "m1"));
        assertThat(r).isEqualTo(new RespInteger(0));
    }

    @Test
    void sismember_exists_returnsOne() {
        sadd.execute(db, array("SADD", "k", "m1"));
        assertThat(sismember.execute(db, array("SISMEMBER", "k", "m1")))
                .isEqualTo(new RespInteger(1));
    }

    @Test
    void sismember_notExists_returnsZero() {
        sadd.execute(db, array("SADD", "k", "m1"));
        assertThat(sismember.execute(db, array("SISMEMBER", "k", "no-such")))
                .isEqualTo(new RespInteger(0));
    }

    @Test
    void sismember_wrongType_returnsWrongType() {
        db.put("k", new RedisHash());
        var r = sismember.execute(db, array("SISMEMBER", "k", "m1"));
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
