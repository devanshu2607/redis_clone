#!/usr/bin/env node

const net = require("net");
const readline = require("readline");

// Connect to the ConC server locally on port 6379
const client = new net.Socket();

client.connect(6379, "localhost", () => {
    console.log("Connected to ConC Cache Server!");
    console.log("Type your commands (e.g. SET key value, GET key). Type EXIT or QUIT to close.\n");
    rl.prompt();
});

// Handle data received from the server
client.on("data", (data) => {
    const response = data.toString().trim();
    
    // Ignore the initial connection handshake message so it doesn't duplicate on screen
    if (response !== "CONNECTED") {
        console.log(response);
    }
    
    rl.prompt();
});

client.on("close", () => {
    console.log("\nConnection closed by server.");
    process.exit(0);
});

client.on("error", (err) => {
    console.error("\nError connecting to server. Is the server running on port 6379?");
    process.exit(1);
});

// Setup interactive terminal prompt
const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout,
    prompt: "ConC-CLI> "
});

rl.on("line", (line) => {
    const cmd = line.trim();
    
    if (cmd.toUpperCase() === "EXIT" || cmd.toUpperCase() === "QUIT") {
        client.destroy();
        process.exit(0);
    }
    
    if (cmd.length > 0) {
        client.write(cmd + "\n");
    } else {
        rl.prompt();
    }
});
