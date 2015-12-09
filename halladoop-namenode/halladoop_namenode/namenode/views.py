from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from namenode.serializers import RegistrationSerializer


class API(APIView):

    """
    List all users, or create a new user.
    """

    def post(self, request, format=None):
        serializer = RegistrationSerializer(data=request.data)
        if serializer.is_valid():
            reg = serializer.create(serializer.validated_data)
            print(reg.nodeID)
            return Response(serializer.data, status=status.HTTP_201_CREATED)
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

    # def delete(self, request, pk, format=None):
    #     user = self.get_object(pk)
    #     user.delete()
    #     return Response(status=status.HTTP_204_NO_CONTENT)
