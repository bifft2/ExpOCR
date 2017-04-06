import time
import requests

_url = 'https://westus.api.cognitive.microsoft.com/vision/v1.0/ocr'
_key = '8ecf5dc0d2e443c2a4b8db3072b17036'
_maxNumRetries = 10


class CVAPI:
    @staticmethod
    def processRequest(json, data, headers, params):
        """
            Helper function to process the request to Project Oxford

            Parameters:
            json: Used when processing images from its URL. See API Documentation
            data: Used when processing image read from disk. See API Documentation
            headers: Used to pass the key information and the data type request
            """

        retries = 0
        result = None

        while True:

            response = requests.request('post', _url, json=json, data=data, headers=headers, params=params)

            if response.status_code == 429:

                print("Message: %s" % (response.json()['error']['message']))

                if retries <= _maxNumRetries:
                    time.sleep(1)
                    retries += 1
                    continue
                else:
                    print('Error: failed after retrying!')
                    break

            elif response.status_code == 200 or response.status_code == 201:

                if 'content-length' in response.headers and int(response.headers['content-length']) == 0:
                    result = None
                elif 'content-type' in response.headers and isinstance(response.headers['content-type'], str):
                    if 'application/json' in response.headers['content-type'].lower():
                        result = response.json() if response.content else None
                    elif 'image' in response.headers['content-type'].lower():
                        result = response.content
            else:
                print("Error code: %d" % (response.status_code))
                print("Message: %s" % (response.json()['error']['message']))

            break

        return result

    @staticmethod
    def send_image_on_disk(data):
        params = {'language': 'en', 'detectOrientation': 'true'}

        headers = dict()
        headers['ocp-apim-subscription-key'] = _key
        headers['Content-Type'] = 'application/octet-stream'

        json = None

        result = CVAPI.processRequest(json, data, headers, params)

        if result is not None:
            print('success in ocr')

        return result