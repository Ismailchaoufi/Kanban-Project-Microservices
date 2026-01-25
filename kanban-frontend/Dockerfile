## ============================================
## Stage 1: Build Angular Application
## ============================================
#FROM node:18-alpine AS build
#
#WORKDIR /app
#
## Copy package files
#COPY package.json package-lock.json ./
#
## Install dependencies
#RUN npm ci --legacy-peer-deps
#
## Copy source code
#COPY . .
#
## Build Angular app for production
#RUN npm run build -- --configuration production
#
## ============================================
## Stage 2: Serve with Nginx
## ============================================
#FROM nginx:alpine
#
## Remove default nginx config and website
#RUN rm -rf /usr/share/nginx/html/* \
#    && rm /etc/nginx/conf.d/default.conf
#
## Copy custom nginx configuration
#COPY nginx.conf /etc/nginx/nginx.conf
#
## Copy built Angular app from build stage
## IMPORTANT: Adjust the path based on your angular.json output path
#COPY --from=build /app/dist/kanban-frontend/browser /usr/share/nginx/html
#
## Create a simple health check
#RUN echo "OK" > /usr/share/nginx/html/health.html
#
## Expose port 80
#EXPOSE 80
#
## Start nginx
#CMD ["nginx", "-g", "daemon off;"]

#For failway
FROM node:18-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci --legacy-peer-deps
COPY . .
RUN npm run build -- --configuration production

FROM nginx:alpine
COPY nginx.conf /etc/nginx/nginx.conf
COPY --from=build /app/dist/kanban-frontend /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
