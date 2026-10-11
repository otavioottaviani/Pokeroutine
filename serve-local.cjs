const http = require("node:http");
const os = require("node:os");
const path = require("node:path");
const fs = require("node:fs");

const root = __dirname;
const host = process.argv[2] || "127.0.0.1";
const port = Number(process.argv[3] || 8765);
const contentTypes = {
  ".html": "text/html; charset=utf-8",
  ".css": "text/css; charset=utf-8",
  ".js": "text/javascript; charset=utf-8",
  ".json": "application/json; charset=utf-8",
  ".webmanifest": "application/manifest+json; charset=utf-8",
  ".svg": "image/svg+xml",
  ".png": "image/png",
  ".ttf": "font/ttf",
  ".woff2": "font/woff2",
};

const server = http.createServer((request, response) => {
  if (request.method !== "GET" && request.method !== "HEAD") {
    response.writeHead(405, { Allow: "GET, HEAD" });
    response.end();
    return;
  }

  let pathname;
  try {
    pathname = decodeURIComponent(new URL(request.url, "http://localhost").pathname);
  } catch {
    response.writeHead(400);
    response.end("Invalid URL");
    return;
  }
  if (pathname === "/") pathname = "/index.html";

  const file = path.resolve(root, `.${pathname}`);
  const relative = path.relative(root, file);
  if (relative.startsWith("..") || path.isAbsolute(relative)) {
    response.writeHead(403);
    response.end("Forbidden");
    return;
  }

  fs.readFile(file, (error, data) => {
    if (error) {
      response.writeHead(error.code === "ENOENT" ? 404 : 500);
      response.end(error.code === "ENOENT" ? "Not found" : "Unable to read file");
      return;
    }
    response.writeHead(200, {
      "Content-Type": contentTypes[path.extname(file).toLowerCase()] || "application/octet-stream",
      "Cache-Control": "no-cache",
      "X-Content-Type-Options": "nosniff",
    });
    response.end(request.method === "HEAD" ? undefined : data);
  });
});

server.listen(port, host, () => {
  console.log(`PokéRoutine: http://localhost:${port}`);
  if (host !== "127.0.0.1" && host !== "localhost") {
    const addresses = Object.values(os.networkInterfaces()).flat().filter((entry) => entry && entry.family === "IPv4" && !entry.internal);
    for (const entry of addresses) console.log(`Na rede local: http://${entry.address}:${port}`);
  }
  console.log("Mantenha esta janela aberta enquanto usa o aplicativo. Ctrl+C encerra o servidor.");
});

server.on("error", (error) => {
  console.error(`Não foi possível iniciar o servidor: ${error.message}`);
  process.exitCode = 1;
});
