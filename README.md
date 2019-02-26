# Search Engine

This project was written during 1-2 month period in 2005 from my dorm room while still studying. I moved it from CD backups to public cloud, as I did not want this to be lost, like I lost android and servlets code for my android app : Flight Flash in 2013

The inspiration to write this, came from reading at that time, an old paper written by Sergey & Larry for Google search engine. I wanted to have first hand experience of writing something similar and tackle those challenges myself. 

Each aspect of writing a search engine can be a research topic by itself, eg Natural Language Processing to understand what user actually wants to search, how to crawl efficiently, how to build link repository, how to build index of words (and apply that to multiple languages) etc etc. I wanted to have a working search engine for myself but within a small 1-2 months time frame while also attending lectures, so had to cut-down the scope of various components.

This has 2 main components : 

## Crawlers 
This components runs multiple threads and crawler multiple websites, always on and always building/updating link/word indexes. 

I made it distributed, so multiple crawlers could be run and I fragmented the oracle database to multiple machines (across my compsci lab :), left them running for multiple nights :) ), so words across various ranges will go to different database instances. Initial strategy is to have words starting with A/a to go to database 1, B/b to go to go database 2, and so on.

## Web
This component is the interface to user. User will see a basic html page with a text box to send their queries. 

If user types more than one word for search, they are searched in parallel in first level LRU cache layer and then hit appropriate database (horizontally sharded) if needed. So, search queries where executed on specific database instance, and load balanced.

Results are then combined together from various sources (cache + database 1 + database 5 + ..etc) and results formatted and returned back to user. 

To be able to return results back to user, every request thread will wait for a few micro/milliseconds before its search results are returned back.

A good deal of emphasis was also given to have multiple layers of components, so any change does not impact other components, eg replacing database from Oracle to Mysql should not impact on other components etc.
## Uses

Tomcat, Servlets, Xml, JDBC (Oracle database), Multithreading



## Contributing
Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

Please make sure to update tests as appropriate.

## License
[MIT](https://choosealicense.com/licenses/mit/)
