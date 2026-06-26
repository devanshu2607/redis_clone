const net = require("net");

class CacheConcClient {
  constructor({ host = "localhost", port = 6379, defaultTtlSeconds = 300 } = {}) {
    this.host = host;
    this.port = port;
    this.defaultTtlSeconds = defaultTtlSeconds;
  }

  async sendCommand(command) {
    return new Promise((resolve, reject) => {
      const socket = new net.Socket();
      let step = 0;

      socket.connect(this.port, this.host, () => {});

      socket.on("data", (data) => {
        const text = data.toString().trim();

        if (step === 0) {
          step = 1;
          socket.write(command + "\n");
          return;
        }

        socket.destroy();
        resolve(text);
      });

      socket.on("error", (err) => {
        reject(err);
      });
    });
  }

  async set(key, value, ttlSeconds = this.defaultTtlSeconds) {
    const command = ttlSeconds > 0
      ? `SET ${key} ${value} EX ${ttlSeconds}`
      : `SET ${key} ${value}`;

    const response = await this.sendCommand(command);
    if (response !== "OK") {
      throw new Error(`SET failed: ${response}`);
    }
  }

  async get(key) {
    const response = await this.sendCommand(`GET ${key}`);
    return response === "(nil)" ? null : response;
  }

  async delete(key) {
    const response = await this.sendCommand(`DEL ${key}`);
    return response === "1";
  }

  async exists(key) {
    const response = await this.sendCommand(`EXISTS ${key}`);
    return response === "1";
  }

  async ttl(key) {
    const response = await this.sendCommand(`TTL ${key}`);
    return Number(response);
  }

  async getOrLoad(key, loaderFn, ttlSeconds = this.defaultTtlSeconds) {
    const cached = await this.get(key);
    if (cached !== null) {
      return cached;
    }

    const loaded = await loaderFn();
    if (loaded !== null && loaded !== undefined) {
      await this.set(key, typeof loaded === "string" ? loaded : JSON.stringify(loaded), ttlSeconds);
    }
    return loaded;
  }
}

module.exports = { CacheConcClient };
