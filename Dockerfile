FROM node:20-alpine
WORKDIR /app

# Install backend dependencies
COPY server/package*.json ./server/
RUN cd server && npm install --production

# Copy application source code and pre-built web assets
COPY . .

# Create uploads directory
RUN mkdir -p server/uploads

ENV PORT=5000
ENV NODE_ENV=production

EXPOSE 5000

CMD ["node", "server/index.js"]
