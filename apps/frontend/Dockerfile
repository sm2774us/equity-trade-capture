FROM node:20-alpine AS build
WORKDIR /workspace
COPY package.json package-lock.json* ./
COPY apps/frontend/package.json apps/frontend/package.json
RUN npm ci
COPY . .
RUN npx nx build frontend --configuration=production

FROM nginx:1.27-alpine
COPY --from=build /workspace/dist/apps/frontend /usr/share/nginx/html
COPY apps/frontend/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
