FROM node:22-alpine AS build
WORKDIR /workspace
# npm workspaces (see root package.json's "workspaces" field) means `npm
# ci` needs every member's package.json present, not just the root one —
# it validates the whole workspace graph against package-lock.json before
# installing anything.
COPY package.json package-lock.json ./
COPY apps/frontend/package.json apps/frontend/package.json
COPY apps/trade-capture-service/package.json apps/trade-capture-service/package.json
COPY apps/pnl-risk-consumer/package.json apps/pnl-risk-consumer/package.json
COPY libs/common-events/package.json libs/common-events/package.json
RUN npm ci --ignore-scripts
COPY . .
RUN npx nx build frontend --configuration=production

FROM nginx:1.27-alpine
COPY --from=build /workspace/dist/apps/frontend /usr/share/nginx/html
COPY apps/frontend/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
