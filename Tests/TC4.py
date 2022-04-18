from http import HTTPStatus
import requests
import time

# Tries to place an order whose total bill exceeds the customer's balance
# The order should not be placed
# Customer's wallet balance should not be deducted


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

	
	#Check customer 301's balance before the invalid request
	http_response = requests.get("http://localhost:8082/balance/301")

	if(http_response.status_code != HTTPStatus.OK):
		test_result = "Fail"

	res_body = http_response.json()
	initial_balance = res_body.get("balance")


	#Place an order with high quantity
	http_response = requests.post("http://localhost:8081/requestOrder", json={"custId":301, "restId":101, "itemId":2, "qty":15})

	if(http_response.status_code != HTTPStatus.CREATED):
		test_result = "Fail"

	time.sleep(2)
	#Check customer 301's balance after the invalid request
	http_response = requests.get("http://localhost:8082/balance/301")

	if(http_response.status_code != HTTPStatus.OK):
		test_result = "Fail"

	res_body = http_response.json()
	after_balance = res_body.get("balance")	


	if initial_balance != after_balance:
		test_result = "Fail"


	return test_result


if __name__ == "__main__":
	test_result = test()
	print(test_result)
