FROM node:22-alpine AS build
WORKDIR /workspace
# Copy the whole repo before installing: with npm workspaces, npm needs
# every workspace member's package.json to resolve the graph correctly.
COPY . .
RUN npm install --ignore-scripts --no-audit --no-fund
RUN npx nx build frontend --configuration=production

FROM nginx:1.27-alpine
COPY --from=build /workspace/dist/apps/frontend /usr/share/nginx/html
COPY apps/frontend/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
