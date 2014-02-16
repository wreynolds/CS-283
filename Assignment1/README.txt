Will Reynolds
2-13-14
CS 283

1. I designed the benchmark client to create 4 threads and send 1000 requests per thread to the server and then I found the average time per request by measuring the total amount of time it took for the server to process all the requests and dividing it by 4000.

2. Single threaded TCP server implementation: 0.33125 ms/request
   Multithreaded TCP server implementation: 0.269 ms/request

3.The multithreaded server implementation performs better than the single threaded server implementation because the multithreaded server can handle and preocess requests as they come on by spaning new threads to handle the work while the single threaded server can only handle requests sequentially one at a time in the order that it receives the requests.