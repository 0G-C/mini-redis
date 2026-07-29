package io.github.ogc.miniredis.command;

import io.github.ogc.miniredis.core.RedisDb;
import io.github.ogc.miniredis.core.object.RedisHash;
import io.github.ogc.miniredis.core.object.RedisString;
import io.github.ogc.miniredis.resp.RespArray;
import io.github.ogc.miniredis.resp.RespBulkString;
import io.github.ogc.miniredis.resp.RespInteger;
import io.github.ogc.miniredis.resp.RespSimpleString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ListCommandTest {

    private final RedisDb db = RedisDb.getInstance();
    private final LpushCommand lpush = new LpushCommand();
    private final RpushCommand rpush = new RpushCommand();
    private final LpopCommand lpop = new LpopCommand();
    private final RpopCommand rpop = new RpopCommand();
    private final LrangeCommand lrange = new LrangeCommand();
    private final LlenCommand llen = new LlenCommand();

    @BeforeEach
    void setUp() {
        db.clear();
    }

    // ==================== LPUSH ====================

    @Test
    void lpush_newKey_createsAndPushes() {
        var r = lpush.execute(db, array("LPUSH", "k", "a"));
        assertThat(r).isEqualTo(new RespInteger(1));
        var r2 = lpush.execute(db, array("LPUSH", "k", "b"));
        assertThat(r2).isEqualTo(new RespInteger(2));
    }

    @Test
    void lpush_order_isLeftmostFirst() {
        lpush.execute(db, array("LPUSH", "k", "a"));
        lpush.execute(db, array("LPUSH", "k", "b"));
        // b 最后插入,在最左: [b, a]
        var range = (RespArray) lrange.execute(db, array("LRANGE", "k", "0", "-1"));
        assertThat(((RespBulkString) range.get(0)).getValue()).isEqualTo("b");
        assertThat(((RespBulkString) range.get(1)).getValue()).isEqualTo("a");
    }

    @Test
    void lpush_wrongType_returnsWrongType() {
        db.put("k", new RedisString("x"));
        var r = lpush.execute(db, array("LPUSH", "k", "a"));
        assertThat(((RespSimpleString) r).getValue()).startsWith("WRONGTYPE");
    }

    // ==================== RPUSH ====================

    @Test
    void rpush_newKey_createsAndPushes() {
        var r = rpush.execute(db, array("RPUSH", "k", "a"));
        assertThat(r).isEqualTo(new RespInteger(1));
    }

    @Test
    void rpush_order_isInsertionOrder() {
        rpush.execute(db, array("RPUSH", "k", "a"));
        rpush.execute(db, array("RPUSH", "k", "b"));
        var range = (RespArray) lrange.execute(db, array("LRANGE", "k", "0", "-1"));
        assertThat(((RespBulkString) range.get(0)).getValue()).isEqualTo("a");
        assertThat(((RespBulkString) range.get(1)).getValue()).isEqualTo("b");
    }

    @Test
    void rpush_wrongType_returnsWrongType() {
        db.put("k", new RedisHash());
        var r = rpush.execute(db, array("RPUSH", "k", "a"));
        assertThat(((RespSimpleString) r).getValue()).startsWith("WRONGTYPE");
    }

    // ==================== LPOP ====================

    @Test
    void lpop_noKey_returnsNull() {
        var r = lpop.execute(db, array("LPOP", "no-key"));
        assertThat(r).isEqualTo(RespBulkString.NULL);
    }

    @Test
    void lpop_normal_removesAndReturnsLeftmost() {
        rpush.execute(db, array("RPUSH", "k", "a"));
        rpush.execute(db, array("RPUSH", "k", "b"));
        var r = lpop.execute(db, array("LPOP", "k"));
        assertThat(r).isEqualTo(new RespBulkString("a"));
        assertThat(llen.execute(db, array("LLEN", "k"))).isEqualTo(new RespInteger(1));
    }

    @Test
    void lpop_emptyList_returnsNull() {
        rpush.execute(db, array("RPUSH", "k", "a"));
        lpop.execute(db, array("LPOP", "k"));  // 清空
        var r = lpop.execute(db, array("LPOP", "k"));
        assertThat(r).isEqualTo(RespBulkString.NULL);
    }

    @Test
    void lpop_wrongType_returnsWrongType() {
        db.put("k", new RedisString("x"));
        var r = lpop.execute(db, array("LPOP", "k"));
        assertThat(((RespSimpleString) r).getValue()).startsWith("WRONGTYPE");
    }

    // ==================== RPOP ====================

    @Test
    void rpop_noKey_returnsNull() {
        var r = rpop.execute(db, array("RPOP", "no-key"));
        assertThat(r).isEqualTo(RespBulkString.NULL);
    }

    @Test
    void rpop_normal_removesAndReturnsRightmost() {
        rpush.execute(db, array("RPUSH", "k", "a"));
        rpush.execute(db, array("RPUSH", "k", "b"));
        var r = rpop.execute(db, array("RPOP", "k"));
        assertThat(r).isEqualTo(new RespBulkString("b"));
        assertThat(llen.execute(db, array("LLEN", "k"))).isEqualTo(new RespInteger(1));
    }

    @Test
    void rpop_wrongType_returnsWrongType() {
        db.put("k", new RedisHash());
        var r = rpop.execute(db, array("RPOP", "k"));
        assertThat(((RespSimpleString) r).getValue()).startsWith("WRONGTYPE");
    }

    // ==================== LRANGE ====================

    @Test
    void lrange_noKey_returnsEmpty() {
        var r = (RespArray) lrange.execute(db, array("LRANGE", "no-key", "0", "-1"));
        assertThat(r.size()).isEqualTo(0);
    }

    @Test
    void lrange_normal_returnsSlice() {
        rpush.execute(db, array("RPUSH", "k", "a"));
        rpush.execute(db, array("RPUSH", "k", "b"));
        rpush.execute(db, array("RPUSH", "k", "c"));
        var r = (RespArray) lrange.execute(db, array("LRANGE", "k", "0", "1"));
        assertThat(r.size()).isEqualTo(2);
        assertThat(((RespBulkString) r.get(0)).getValue()).isEqualTo("a");
        assertThat(((RespBulkString) r.get(1)).getValue()).isEqualTo("b");
    }

    @Test
    void lrange_negativeIndex_fromEnd() {
        rpush.execute(db, array("RPUSH", "k", "a"));
        rpush.execute(db, array("RPUSH", "k", "b"));
        rpush.execute(db, array("RPUSH", "k", "c"));
        var r = (RespArray) lrange.execute(db, array("LRANGE", "k", "-2", "-1"));
        assertThat(r.size()).isEqualTo(2);
        assertThat(((RespBulkString) r.get(0)).getValue()).isEqualTo("b");
        assertThat(((RespBulkString) r.get(1)).getValue()).isEqualTo("c");
    }

    @Test
    void lrange_startGreaterThanStop_returnsEmpty() {
        rpush.execute(db, array("RPUSH", "k", "a"));
        var r = (RespArray) lrange.execute(db, array("LRANGE", "k", "5", "10"));
        assertThat(r.size()).isEqualTo(0);
    }

    @Test
    void lrange_wrongType_returnsWrongType() {
        db.put("k", new RedisString("x"));
        var r = lrange.execute(db, array("LRANGE", "k", "0", "-1"));
        assertThat(((RespSimpleString) r).getValue()).startsWith("WRONGTYPE");
    }

    // ==================== LLEN ====================

    @Test
    void llen_noKey_returnsZero() {
        var r = llen.execute(db, array("LLEN", "no-key"));
        assertThat(r).isEqualTo(new RespInteger(0));
    }

    @Test
    void llen_normal_returnsCount() {
        rpush.execute(db, array("RPUSH", "k", "a"));
        rpush.execute(db, array("RPUSH", "k", "b"));
        assertThat(llen.execute(db, array("LLEN", "k"))).isEqualTo(new RespInteger(2));
    }

    @Test
    void llen_wrongType_returnsWrongType() {
        db.put("k", new RedisHash());
        var r = llen.execute(db, array("LLEN", "k"));
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
