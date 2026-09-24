FROM node:22-alpine AS build
WORKDIR /workspace
# Copy the whole repo before installing: with npm workspaces, `npm ci`
# needs every workspace member's package.json to validate against
# package-lock.json, and selectively COPYing just those files has proven
# fragile (easy to miss one, or for a path to drift as the workspace list
# changes). Copying everything up front costs some Docker layer-cache
# efficiency — any source change invalidates this layer too — but that
# trade is worth it for a build that can't fail this way again.
COPY . .
RUN npm ci --ignore-scripts
RUN npx nx build frontend --configuration=production

FROM nginx:1.27-alpine
COPY --from=build /workspace/dist/apps/frontend /usr/share/nginx/html
COPY apps/frontend/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
