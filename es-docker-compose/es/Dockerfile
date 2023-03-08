FROM --platform=linux/amd64 elasticsearch:7.6.0

# 设置配置文件
COPY ./elasticsearch.yml /usr/share/elasticsearch/config/

# 安装插件
COPY ./redis-plugin-1.0.zip /
RUN /usr/share/elasticsearch/bin/elasticsearch-plugin install --batch file:///redis-plugin-1.0.zip
