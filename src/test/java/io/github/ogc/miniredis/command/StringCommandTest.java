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

class StringCommandTest {

    private final RedisDb db = RedisDb.getInstance();
    private final IncrCommand incr = new IncrCommand();
    private final DecrCommand decr = new DecrCommand();
    private final AppendCommand append = new AppendCommand();
    private final StrlenCommand strlen = new StrlenCommand();
    private final ExpireCommand expire = new ExpireCommand();

    @BeforeEach
    void setUp() {
        db.clear();
    }

    // ==================== INCR ====================

    @Test
    void incr_newKey_returnsOne() {
        var r = incr.execute(db, array("INCR", "k"));
        assertThat(r).isEqualTo(new RespInteger(1));
        assertThat(db.get("k")).satisfies(o ->
                assertThat(((RedisString) o).getValue()).isEqualTo("1"));
    }

    @Test
    void incr_existing_increments() {
        db.put("k", new RedisString("5"));
        var r = incr.execute(db, array("INCR", "k"));
        assertThat(r).isEqualTo(new RespInteger(6));
        assertThat(((RedisString) db.get("k")).getValue()).isEqualTo("6");
    }

    @Test
    void incr_nonInteger_returnsError() {
        db.put("k", new RedisString("abc"));
        var r = incr.execute(db, array("INCR", "k"));
        assertThat(r).isInstanceOf(RespSimpleString.class);
        assertThat(((RespSimpleString) r).getValue())
                .startsWith("ERR value is not an integer");
    }

    @Test
    void incr_wrongType_returnsWrongType() {
        db.put("k", new RedisHash());
        var r = incr.execute(db, array("INCR", "k"));
        assertThat(r).isInstanceOf(RespSimpleString.class);
        assertThat(((RespSimpleString) r).getValue())
                .startsWith("WRONGTYPE");
    }

    // ==================== DECR ====================

    @Test
    void decr_newKey_returnsMinusOne() {
        var r = decr.execute(db, array("DECR", "k"));
        assertThat(r).isEqualTo(new RespInteger(-1));
        assertThat(((RedisString) db.get("k")).getValue()).isEqualTo("-1");
    }

    @Test
    void decr_existing_decrements() {
        db.put("k", new RedisString("5"));
        var r = decr.execute(db, array("DECR", "k"));
        assertThat(r).isEqualTo(new RespInteger(4));
        assertThat(((RedisString) db.get("k")).getValue()).isEqualTo("4");
    }

    @Test
    void decr_nonInteger_returnsError() {
        db.put("k", new RedisString("abc"));
        var r = decr.execute(db, array("DECR", "k"));
        assertThat(((RespSimpleString) r).getValue())
                .startsWith("ERR value is not an integer");
    }

    // ==================== APPEND ====================

    @Test
    void append_newKey_createsAndReturnsLength() {
        var r = append.execute(db, array("APPEND", "k", "hello"));
        assertThat(r).isEqualTo(new RespInteger(5));
        assertThat(((RedisString) db.get("k")).getValue()).isEqualTo("hello");
    }

    @Test
    void append_existing_appendsAndReturnsTotalLength() {
        db.put("k", new RedisString("hello"));
        var r = append.execute(db, array("APPEND", "k", " world"));
        assertThat(r).isEqualTo(new RespInteger(11));
        assertThat(((RedisString) db.get("k")).getValue()).isEqualTo("hello world");
    }

    @Test
    void append_wrongType_returnsWrongType() {
        db.put("k", new RedisHash());
        var r = append.execute(db, array("APPEND", "k", "x"));
        assertThat(((RespSimpleString) r).getValue()).startsWith("WRONGTYPE");
    }

    // ==================== STRLEN ====================

    @Test
    void strlen_noKey_returnsZero() {
        var r = strlen.execute(db, array("STRLEN", "no-such-key"));
        assertThat(r).isEqualTo(new RespInteger(0));
    }

    @Test
    void strlen_normal_returnsLength() {
        db.put("k", new RedisString("hello"));
        var r = strlen.execute(db, array("STRLEN", "k"));
        assertThat(r).isEqualTo(new RespInteger(5));
    }

    @Test
    void strlen_wrongType_returnsWrongType() {
        db.put("k", new RedisHash());
        var r = strlen.execute(db, array("STRLEN", "k"));
        assertThat(((RespSimpleString) r).getValue()).startsWith("WRONGTYPE");
    }

    // ==================== EXPIRE ====================

    @Test
    void expire_noKey_returnsZero() {
        var r = expire.execute(db, array("EXPIRE", "no-such-key", "100"));
        assertThat(r).isEqualTo(new RespInteger(0));
    }

    @Test
    void expire_normal_setsExpireAtAndReturnsOne() {
        db.put("k", new RedisString("v"));
        var r = expire.execute(db, array("EXPIRE", "k", "100"));
        assertThat(r).isEqualTo(new RespInteger(1));
        // expireAt 应设为未来(当前时间 + 100s)
        assertThat(db.get("k")).isNotNull();  // key 还没过期的…
    }

    @Test
    void expire_zeroSeconds_deletesKey() {
        db.put("k", new RedisString("v"));
        var r = expire.execute(db, array("EXPIRE", "k", "0"));
        assertThat(r).isEqualTo(new RespInteger(1));
        assertThat(db.get("k")).isNull();  // 已删
    }

    // ==================== helper ====================

    /** 构造 RESP Array,第一元素是命令名,其余是参数。 */
    private static RespArray array(String cmd, String... args) {
        RespBulkString[] elements = new RespBulkString[1 + args.length];
        elements[0] = new RespBulkString(cmd);
        for (int i = 0; i < args.length; i++) {
            elements[i + 1] = new RespBulkString(args[i]);
        }
        return new RespArray(List.of(elements));
    }
}
