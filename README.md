# redis数据源插件

让ES使用Redis中的数据进行搜索排序

Welcome PR，Welcome Issues!

# 快速开始

1、安装插件

`elasticsearch-plugin install --batch file:///redis-plugin-1.0.zip`

2、elasticsearch.yml中添加redis连接信息
`esHotelPlugin.redis.url: "redis://127.0.0.1:6379"`

3、使用示例

```
PUT /demo
{
    "mappings": {
        "properties": {
            "product_id": {
                "type": "long"
            },
            "product_name": {
                "type": "text"
            }
        }
    }
}


PUT /demo/_doc/1
{
    "product_id":1,
    "product_name":"[国航]北京至三亚"
}

PUT /demo/_doc/2
{
    "product_id":2,
    "product_name":"[东航]北京至三亚"
}
```

执行查询

```
GET /demo/_search
{
  "query": {
    "bool": {
      "filter": {
        "script": {
          "script": {
            "source": "filter",
            "lang": "redis",
            "params": {
              "lt": "100",                  # 过滤条件支持lt gt eq lte gte 分别表示小于，大于，等于，小于等于，大于等于
              "gt": "300",
              "key_pre": "20230101_price_", # redis key前缀
              "field": "product_id"         # 在redis key中拼接文档字段内容
            }
          }
        }
      }
    }
  }
}
```

查询与排序

```
GET /demo/_search
{
  "query": {
    "function_score": {
      "query": {
        "bool": {
          "filter": [
            {
              "script": {
                "script": {
                  "source": "filter",
                  "lang": "redis",
                  "params": {
                    "gt": "100",
                    "lt": "300",
                    "key_pre": "20230101_price_",
                    "field": "product_id"
                  }
                }
              }
            }
          ]
        }
      },
      "boost_mode": "replace",
      "functions": [
        {
          "script_score": {           # 使用redis值作为文档得分
            "script": {
              "source": "score",
              "lang": "redis",
              "params": {
                "field": "product_id",
                "key_pre": "20230101_price_"
              }
            }
          }
        }
      ]
    }
  }
}
```

向redis中插入数据，再测试

```
set 20230101_price_1 200
set 20230101_price_2 250
```

# Develop

1、编译插件

```
mvn clean package
```

2、测试插件

```
cp target/redis-plugin-1.0.zip es-docker-compose/es/
cd es-docker-compose && docker-compose up --build -d
```

# 更复杂的动态数据查询

你可以自己基于Redis协议实现一个查询器，将你的动态数据封装成redis GET请求的查询形式。这样即可实现各种复杂逻辑下的动态数据排序、过滤

# Thanks:

https://www.jianshu.com/p/dca50c37fab7

https://github.com/BigDataBoutique/elasticsearch-rescore-redis`