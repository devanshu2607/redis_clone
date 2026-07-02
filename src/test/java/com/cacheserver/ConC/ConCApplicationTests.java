package com.cacheserver.ConC;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.cacheserver.ConC.command.Command;
import com.cacheserver.ConC.command.CommandType;
import com.cacheserver.ConC.parser.PlainTextParser;
import com.cacheserver.ConC.router.RequestRouter;

class ConCApplicationTests {

    @Test
    void testParserAndRouter() {
        PlainTextParser parser = new PlainTextParser();
        Command command = parser.parse("SET user1 Rahul EX 30");

        assertEquals(CommandType.SET, command.getType());
        assertEquals("user1", command.getKey());
        assertEquals(3, command.getArgs().length - 1);

        RequestRouter router = new RequestRouter(3);
        int workerIndex = router.getWorkerIndex(command);
        assertTrue(workerIndex >= 0 && workerIndex < 3);
    }
}
