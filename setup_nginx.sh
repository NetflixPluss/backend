#!/bin/bash

#Atualizar packages
sudo apt update

if ! command -v nginx &> /dev/null
then
  echo "NGINX not found. Installing..."
  sudo apt install -y nginx
else
  echo "NGINX found."
fi

if ! command -v ffmpeg &> /dev/null
then
  echo "FFmpeg not found. Installing..."
  sudo apt install -y ffmpeg
else
  echo "FFmpeg found."
fi

if ! command -v mvn &> /dev/null; then
  echo "Maven not found. Installing..."
  sudo apt install -y maven
else
  echo "Maven found."
fi

HLSHD=/var/www/netflixplus/hls/1080p
HLSSD=/var/www/netflixplus/hls/360p

for dir in "$HLSHD" "$HLSSD"; do
  if ! [ -d "$dir" ]; then
    echo "Creating HLS directory $dir..."
    sudo mkdir -p "$dir"
    sudo chown -R "$USER":"$USER" "$dir"
    sudo chmod 755 "$dir"
  else
    echo "HLS directory $dir already exists."
  fi
done

sudo cp ./nginx.conf /etc/nginx/

if systemctl is-active --quiet nginx; then
    echo "NGINX already running reloading config only."
    sudo nginx -s reload
else
    echo "NGINX starting"
    sudo nginx -t && sudo systemctl restart nginx
fi

echo "NGINX started with success!"

echo "Building backend with Maven..."
mvn clean package || { echo "Maven build failed"; exit 1; }

echo "Moving backend JAR to build/ directory..."
mv target/netflixplusbackend.jar build/netflixplusbackend.jar