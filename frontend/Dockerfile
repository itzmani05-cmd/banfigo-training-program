# Build the React app, then serve the static files with Nginx.
# The VITE_* values are baked into the bundle at build time (docker-compose.yml passes them as build args).

FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .

ARG VITE_API_BASE_URL=/api
ARG VITE_KEYCLOAK_URL=/auth
ARG VITE_KEYCLOAK_REALM=BanfigoNew
ARG VITE_KEYCLOAK_CLIENT_ID=BanfigoFrontend
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL \
    VITE_KEYCLOAK_URL=$VITE_KEYCLOAK_URL \
    VITE_KEYCLOAK_REALM=$VITE_KEYCLOAK_REALM \
    VITE_KEYCLOAK_CLIENT_ID=$VITE_KEYCLOAK_CLIENT_ID
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx-spa.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
