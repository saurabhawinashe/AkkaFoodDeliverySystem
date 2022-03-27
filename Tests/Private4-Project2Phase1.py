from http import HTTPStatus
from threading import Thread
import requests

# Check if order id which does not exist returns 404 status or not

# RESTAURANT SERVICE    : http://localhost:8080
# DELIVERY SERVICE      : http://localhost:8081
# WALLET SERVICE        : http://localhost:8082


def t1(result):  # First concurrent request

    # Customer 301 requests an order of item 1, quantity 3 from restaurant 101
    http_response = requests.get(
        f"http://localhost:8081/order/1000")

    result["1"] = http_response


def t2(result):  # Second concurrent request

    # Customer 302 requests an order of item 1, quantity 3 from restaurant 101
    http_response = requests.get(
        f"http://localhost:8081/order/1001")

    result["2"] = http_response

def test():

    result = {}

    # Reinitialize Restaurant service
    http_response = requests.post("http://localhost:8080/reInitialize")

    # Reinitialize Delivery service
    http_response = requests.post("http://localhost:8081/reInitialize")

    # Reinitialize Wallet service
    http_response = requests.post("http://localhost:8082/reInitialize")
        
    thread1 = Thread(target=t1, kwargs={"result": result})
    thread2 = Thread(target=t2, kwargs={"result": result})

    thread1.start()
    thread2.start()

    thread1.join()
    thread2.join()

    status_code1 = result["1"].status_code
    status_code2 = result["2"].status_code

    if(status_code1 != HTTPStatus.NOT_FOUND and status_code2 != HTTPStatus.NOT_FOUND):
        return 'Fail'

    return 'Pass'


if __name__ == "__main__":

    print(test())
