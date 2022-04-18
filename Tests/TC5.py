# Balance consistency check
# Checks if the balance is consistent after placing
# multiple orders and also balance should not be
# negative.
#Note :- initialData.txt which was provided initially is followed for the data, a new initialData.txt may not work as prices
#         of item id might me different, If you are providing new initialData.txt make sure to change the price variable

from http import HTTPStatus
import requests
import time

# RESTAURANT SERVICE    : http://localhost:8080
# DELIVERY SERVICE      : http://localhost:8081
# WALLET SERVICE        : http://localhost:8082

def test():
	test_result = 'Pass'

	# Reinitialize Restaurant service
	http_response = requests.post("http://localhost:8080/reInitialize")

	if(http_response.status_code != HTTPStatus.CREATED):
		test_result = 'Fail'

    # Reinitialize Delivery service
	http_response = requests.post("http://localhost:8081/reInitialize")

	if(http_response.status_code != HTTPStatus.CREATED):
		test_result = 'Fail'

	# Reinitialize Wallet service
	http_response = requests.post("http://localhost:8082/reInitialize")

	if(http_response.status_code != HTTPStatus.CREATED):
		test_result = 'Fail'	

	# Check Customer 301 Balance
	http_response = requests.get("http://localhost:8082/balance/301")

	if(http_response.status_code != HTTPStatus.OK):
		test_result = 'Fail'

	res_body = http_response.json()

	cust_id = res_body.get("custId")
	initialBalance = res_body.get("balance")

	if(cust_id!=301 or initialBalance<0):
		test_result = 'Fail'

	# Agent 201 sign in
	http_response = requests.post("http://localhost:8081/agentSignIn", json={"agentId": 201})

	if(http_response.status_code != HTTPStatus.CREATED):
		test_result = 'Fail'

	# Agent 202 sign in
	http_response = requests.post("http://localhost:8081/agentSignIn", json={"agentId": 202})

	if(http_response.status_code != HTTPStatus.CREATED):
		test_result = 'Fail'
	
	time.sleep(3)
	# Customer 301 places order from restId 101 and itemId 1 and quantity = 3 which has price 180 per quantity
	http_response = requests.post(
		"http://localhost:8081/requestOrder", json={"custId": 301, "restId": 101, "itemId": 1, "qty": 3})

	res_body = http_response.json()
	order_id = -1
	if(http_response.status_code != HTTPStatus.CREATED):
		test_result = 'Fail'
	else:
		order_id = res_body.get("orderId")

	if (order_id == -1):
		test_result = 'Fail'

	# Customer 301 places another order from restId 102 and itemId 4 and quantity = 2 which has price 45 per quantity
	http_response = requests.post(
		"http://localhost:8081/requestOrder", json={"custId": 301, "restId": 102, "itemId": 4, "qty": 2})

	res_body = http_response.json()
	order_id = -1
	if(http_response.status_code != HTTPStatus.CREATED):
		test_result = 'Fail'
	else:
		order_id = res_body.get("orderId")

	if (order_id == -1):
		test_result = 'Fail'
	
	time.sleep(3)
	# Check 301 wallet balance, it should be 540+90 = 630 less than the original balance
	http_response = requests.get("http://localhost:8082/balance/301")

	if(http_response.status_code != HTTPStatus.OK):
		test_result = 'Fail'

	res_body = http_response.json()

	cust_id = res_body.get("custId")
	laterBalance = res_body.get("balance")

	price = 630

	if(cust_id!=301 or laterBalance<0):
		test_result = 'Fail'

	if(initialBalance - laterBalance != price):
		test_result = 'Fail'


	return test_result


if __name__ == "__main__":
	test_result = test()
	print(test_result)
